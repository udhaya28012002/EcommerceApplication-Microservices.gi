package org.webapp.ecommerce.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.client.OrderServiceClient;
import org.webapp.ecommerce.dto.kafkadto.OrderEvent;
import org.webapp.ecommerce.dto.kafkadto.RefundRequestedEvent;
import org.webapp.ecommerce.dto.request.OrderItemRequestDto;
import org.webapp.ecommerce.dto.request.PlaceOrderRequest;
import org.webapp.ecommerce.dto.response.*;
import org.webapp.ecommerce.entity.OrderItems;
import org.webapp.ecommerce.entity.OrderStatus;
import org.webapp.ecommerce.entity.Orders;
import org.webapp.ecommerce.entity.PaymentStatus;
import org.webapp.ecommerce.exception.*;
import org.webapp.ecommerce.kafka.KafkaService;
import org.webapp.ecommerce.repository.OrderRepository;
import org.webapp.ecommerce.util.CurrentUserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.LocalDateTime.now;

@Service
public class OrderService {

    private final OrderRepository orderServiceRepository;
    private final CurrentUserService currentUserService;
    private final OrderServiceClient orderServiceClient;
    private final OrderStatusService orderStatusService;
    private final KafkaService kafkaService;

    @Value("${payment.currencyCode}")
    private String currencyCode;

    private static final int DEFAULT_DISCOUNT = 0;

    private final Logger log = LoggerFactory.getLogger(OrderService.class);

    public OrderService(OrderRepository orderServiceRepository, CurrentUserService currentUserService, OrderServiceClient orderServiceClient, OrderStatusService orderStatusService, KafkaService kafkaService) {
        this.orderServiceRepository = orderServiceRepository;
        this.currentUserService = currentUserService;
        this.orderServiceClient = orderServiceClient;
        this.orderStatusService = orderStatusService;
        this.kafkaService = kafkaService;
    }

    @Transactional
    public OrderEvent placeOrderReq(PlaceOrderRequest placeOrderRequest) throws OrderItemsNotFoundException {

        int discount = 0;

        double deliveryCharge = 0;

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Order placement started for user: {}", loggedUser);

        if (placeOrderRequest.getItems() == null || placeOrderRequest.getItems().isEmpty()) {

            log.warn("Order items missing for user: {}", loggedUser);

            throw new OrderItemsNotFoundException("Order info is missing");
        }

        LocalDateTime today = now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        Orders order = new Orders();
        order.setOrderDate(today);
        order.setUsername(loggedUser);
        order.setOrderNumber(generateOrderNumber());

        log.info("Generated order number: {}", order.getOrderNumber());

        List<OrderItems> orderItemsList = buildOrderItems(loggedUser, placeOrderRequest, order, discount, deliveryCharge, role);

        double totalPricePerOrder = orderItemsList.stream()
                .mapToDouble(OrderItems::getTotalPrice)
                .sum();

        log.info("Total order price before discount: {}", totalPricePerOrder);

        ApplyCouponResponse applyCouponResponse = orderServiceClient.checkCouponsAndRedeem(loggedUser, placeOrderRequest.getCouponCode(), totalPricePerOrder, role);// Need to Invoke From Discount Service:

        Long finalPrice = applyCouponResponse.getFinalPrice();
        order.setFinalPrice(finalPrice);

        order.setAppliedCoupon(applyCouponResponse.getCouponName());

        order.setOrderItemsList(orderItemsList);

        log.info("Final order price after discount: {}", finalPrice);

        UserDetailsDtoResponse userDetailsDtoResponse = orderServiceClient.getUserDetails(loggedUser);

        // PAYMENT PAGE (IF SUCCESS WE NEED TO PLACE ORDER OTHERWISE REVERT),

        //Creating Payment Intent:
        PaymentResponse paymentResponse1 = orderServiceClient.initiatePaymentIntent(
                order.getOrderNumber(),
                loggedUser,
                role,
                finalPrice,
                currencyCode
        );

        String stripePaymentIntentId = paymentResponse1.getStripePaymentIntentId();

        order.setStripePaymentIntentId(stripePaymentIntentId);

        order.setOrderStatus(OrderStatus.CREATED);

        Orders savedOrder = orderServiceRepository.save(order);

        log.debug("Order is set to Status : {} and waiting for payment for OrderId : {}", OrderStatus.CREATED, order.getOrderNumber());

        //Confirming Payment Intent:
        PaymentResponse paymentResponse2 = orderServiceClient.confirmPaymentProcess(loggedUser, role, paymentResponse1.getPaymentId());

        log.debug("Payment Id : {} is on status : {} and waiting for the payment to be succeeded", paymentResponse2.getPaymentId(), paymentResponse2.getStatus());

        OrderEvent orderEvent = buildOrderEvent(order, paymentResponse2, userDetailsDtoResponse);

        //SENDING TO KAFKA TOPIC AS A PRODUCER
        kafkaService.sendMessage(
                orderEvent,
                "order.created",
                order.getOrderNumber()
        );

        return orderEvent;
    }

    private String generateOrderNumber() {
        return "ORD-%s-%s".formatted(
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE),
                UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase()
        );
    }

    @Transactional
    public OrderEvent confirmOrder(PaymentResponse paymentResponse) {

        Orders order = orderServiceRepository.findByOrderNumber(paymentResponse.getOrderId());

        if (order == null) {
            throw new OrderNotFoundException("Order not found: " + paymentResponse.getOrderId());
        }

        if (!paymentResponse.getOrderId().equals(order.getOrderNumber())) {
            throw new OrderProcessingException("Payment does not belong to this order.");
        }

        if (Double.compare(paymentResponse.getAmount(), order.getFinalPrice()) != 0){
            throw new OrderProcessingException("Payment does not belong to this order.");
        }

        UserDetailsDtoResponse userDetailsDtoResponse = orderServiceClient.getUserDetails(order.getUsername());

        // ── Idempotency guard — protects against Stripe webhook retries ─────────

        if (order.getOrderStatus() == OrderStatus.CONFIRMED) {
            log.warn("Duplicate payment confirmation ignored. Order={}", order.getOrderNumber());

            return buildOrderEvent(order, paymentResponse, userDetailsDtoResponse);
        }

        if (order.getOrderStatus() == OrderStatus.FAILED) {
            log.warn("Late payment confirmation ignored. Order={}", order.getOrderNumber());

            return buildOrderEvent(order, paymentResponse, userDetailsDtoResponse);
        }

        // ---------------- Payment Failed ----------------

        if (paymentResponse.getStatus() != PaymentStatus.SUCCEEDED) {

            log.info("Payment failed for order {}", order.getOrderNumber());

            orderStatusService.markOrderAsFailed(order.getOrderNumber());

            OrderEvent event = buildOrderEvent(order, paymentResponse, userDetailsDtoResponse);
            event.setOrderStatus(OrderStatus.FAILED.name());
            event.setReason("PAYMENT_FAILED");

            kafkaService.sendMessage(
                    event,
                    "order.failed",
                    order.getOrderNumber()
            );

            return event;
        }

        // ---------------- Inventory ----------------

        try {

            updatingStocks(
                    order.getUsername(),
                    order.getOrderItemsList(),
                    "ROLE_CUSTOMER",
                    order.getOrderNumber()
            );

            log.info("Inventory updated successfully for order {}", order.getOrderNumber());

        } catch (InventoryUpdateFailedException ex) {

            log.warn("Inventory update failed for order {} : {}",
                    order.getOrderNumber(),
                    ex.getMessage());

            orderStatusService.markOrderAsFailed(order.getOrderNumber());

            OrderEvent event = buildOrderEvent(order, paymentResponse, userDetailsDtoResponse);
            event.setOrderStatus(OrderStatus.FAILED.name());
            event.setReason("OUT_OF_STOCK_REFUND_INITIATED");

            RefundRequestedEvent refundEvent = new RefundRequestedEvent(
                    UUID.randomUUID().toString(),
                    order.getOrderNumber(),
                    paymentResponse.getPaymentId(),
                    paymentResponse.getStripePaymentIntentId(),
                    paymentResponse.getAmount(),
                    paymentResponse.getCurrency(),
                    order.getUsername(),
                    "OUT_OF_STOCK",
                    LocalDateTime.now());


            kafkaService.sendMessage(
                    refundEvent,
                    "payment.refund",
                    refundEvent.getOrderNumber()
            );

            kafkaService.sendMessage(
                    event,
                    "order.failed",
                    order.getOrderNumber()
            );

            return event;
        }

        // ---------------- Success ----------------

        order.setOrderStatus(OrderStatus.CONFIRMED);

        Orders savedOrder = orderServiceRepository.save(order);

        log.info("Order confirmed successfully. Order={}, User={}",
                savedOrder.getOrderNumber(),
                savedOrder.getUsername());

        OrderEvent confirmedEvent =
                buildOrderEvent(savedOrder, paymentResponse, userDetailsDtoResponse);

        kafkaService.sendMessage(
                confirmedEvent,
                "order.confirmed",
                savedOrder.getOrderNumber()
        );

        return confirmedEvent;
    }

    private OrderEvent buildOrderEvent(Orders order, PaymentResponse payment, UserDetailsDtoResponse user) {

        return new OrderEvent(
                user.getName(),
                user.getEmailId(),
                user.getContactNo(),
                order.getUsername(),
                payment.getOrderId(),
                order.getOrderStatus().name(),
                payment.getStatus().name(),
                payment.getPaymentId(),
                payment.getStripePaymentIntentId(),
                payment.getAmount()
        );
    }


    public void checkOut() {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Checkout started for user: {}", loggedUser);

        //Get the Cart From Cart-Service for the logged User.
        CartResponseDto cart = orderServiceClient.getCart(loggedUser, role);


        if (cart == null || cart.getCartItemsCategoryResponseDtoList().isEmpty()) {

            log.warn("Cart is empty for user: {}", loggedUser);

            throw new IllegalArgumentException("No products available in cart");
        }

        PlaceOrderRequest prepareOrderReq = new PlaceOrderRequest();

        for (CartCategoryResponseDto cartCategory : cart.getCartItemsCategoryResponseDtoList()) {
            prepareOrderReq.setItems(prepareOrderFromCart(cartCategory.getCartItemsResponseDtoList()));
        }

        placeOrderReq(prepareOrderReq);

        log.info("Checkout completed successfully for user: {}", loggedUser);
    }

    /*private ApplyCouponResponse checkCouponsAndRedeem(String coupon, double totalPricePerOrder) {
        log.debug("Validating coupon: {}", coupon);


        if (!discountService.validateCoupon(coupon)) {

            log.info("Coupon Code Is Not Available");

            ApplyCouponResponse couponResponse = new ApplyCouponResponse();
            couponResponse.setApplied(false);
            couponResponse.setMessage("No Coupon is applied");
            couponResponse.setFinalPrice(totalPricePerOrder);
            return couponResponse;
        }

        ApplyCouponResponse applyCouponResponse = discountService.applyDiscountByUsers(coupon, totalPricePerOrder);

        if (!applyCouponResponse.isApplied()) {

            log.warn("Coupon application failed: {}", applyCouponResponse.getMessage());

            log.info(applyCouponResponse.getMessage());
        }

        return applyCouponResponse;
    }*/

    private List<OrderItemRequestDto> prepareOrderFromCart(List<CartItemsResponseDto> cartItems) {

        List<OrderItemRequestDto> orderItemsList = new ArrayList<>();

        for (CartItemsResponseDto cartItem : cartItems) {
            OrderItemRequestDto orderItemRequestDto = new OrderItemRequestDto();
            orderItemRequestDto.setProductId(cartItem.getProductId());
            orderItemRequestDto.setQuantity(cartItem.getQuantity());
            orderItemsList.add(orderItemRequestDto);
        }

        return orderItemsList;
    }


    public OrdersResDto getOrderByOrderNumber(String orderNumber) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Fetching order details. User: {}, OrderNumber: {}", loggedUser, orderNumber);

        Orders orders = orderServiceRepository.findByOrderNumber(orderNumber);

        if (orders == null) {

            log.warn("Order not found. OrderNumber: {}", orderNumber);

            throw new OrderNotFoundException("No Orders present with this OrderNumber : " + orderNumber);
        }

        log.info("Order fetched successfully. OrderNumber: {}", orderNumber);

        return buildOrderResDto(orders);
    }

    public List<OrdersResDto> getUserOrdersByUserName() {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Fetching all orders for user: {}", loggedUser);

        List<Orders> orders = orderServiceRepository.findByUsername(loggedUser);

        if (orders.isEmpty()) {

            log.warn("No orders found for user: {}", loggedUser);

            throw new OrderNotFoundException("No orders found.");
        }

        log.info("Total orders fetched for user {} : {}", loggedUser, orders.size());

        return orders.stream()
                .map(this::buildOrderResDto)
                .toList();
    }

    /*public List<OrdersResDto> getUserOrdersByEmailId(String emailId) {

        String loggedUser = currentUserService.getLoggedInUser();

        if(!userRepo.findByUserName(loggedUser).getEmailId().equals(emailId)){
            throw new AccessDeniedException("Access Denied");
        }

        List<Orders> orders = orderServiceRepository.findByUsers_EmailId(emailId);

        if (orders.isEmpty()) {
            throw new OrderNotFoundException("No orders found.");
        }

        return orders.stream()
                .map(this::buildOrderResDto)
                .toList();
    }

    public List<OrdersResDto> getUserOrdersByContactNo(String contactNo) {
        List<Orders> orders = orderServiceRepository.findByUsers_ContactNo(contactNo);

        if (orders.isEmpty()) {
            throw new OrderNotFoundException("No orders found.");
        }

        return orders.stream()
                .map(this::buildOrderResDto)
                .toList();
    }*/

    public List<AdminOrdersResDto> getAllOrders() {

        log.debug("Fetching all orders for admin");

        List<Orders> ordersList = orderServiceRepository.findAll();

        if (ordersList.isEmpty()) {

            log.warn("No orders found in database");

            throw new OrderNotFoundException("No orders found.");
        }

        log.info("Total orders fetched: {}", ordersList.size());

        return ordersList.stream()
                .map(this::buildOrderResDtoForAdmin)
                .toList();
    }


    public List<String> filterUsernamesByOrderAmt(double orderAmt) {
        return orderServiceRepository.findUsernamesByFilterPrice(orderAmt);
    }

    @Transactional
    public String cancelOrder(String orderNumber) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Cancel order request initiated. User: {}, OrderNumber: {}", loggedUser, orderNumber);

        Orders orders = getOrder(orderNumber);

        orderStatusService.validateOrderStatusUpdate(orders.getOrderStatus(), OrderStatus.CANCELLED);

        // Reversible local-system actions first
        revertInventory(orders);

        log.info("Inventory reverted successfully for cancelled order: {}", orderNumber);

        // Revert coupon (if applicable)
        if (!"No Coupon is used".equalsIgnoreCase(orders.getAppliedCoupon())) {

            String message = orderServiceClient.revertCoupon(
                    loggedUser,
                    orders.getAppliedCoupon(),
                    role
            );

            log.info("{} for cancelled order: {}", message, orderNumber);

        } else {

            log.info("No coupon applied for order {}", orderNumber);
        }

        // Update order status
        orders.setOrderStatus(OrderStatus.CANCELLED);

        Orders savedOrder = orderServiceRepository.save(orders);

        log.info("Order cancelled successfully. OrderNumber={}",
                savedOrder.getOrderNumber());

        UserDetailsDtoResponse user =
                orderServiceClient.getUserDetails(loggedUser);

        // Publish refund request
        RefundRequestedEvent refundEvent = new RefundRequestedEvent(
                UUID.randomUUID().toString(),
                savedOrder.getOrderNumber(),
                null, // paymentId (optional)
                savedOrder.getStripePaymentIntentId(),
                savedOrder.getFinalPrice(),
                currencyCode,
                savedOrder.getUsername(),
                "ORDER_CANCELLED",
                LocalDateTime.now()
        );

        kafkaService.sendMessage(
                refundEvent,
                "payment.refund",
                savedOrder.getOrderNumber()
        );

        // Step 5 - Publish cancellation event

        OrderEvent cancelledEvent = new OrderEvent(
                user.getName(),
                user.getEmailId(),
                user.getContactNo(),
                user.getUserName(),
                savedOrder.getOrderNumber(),
                savedOrder.getOrderStatus().name(),
                (long) savedOrder.getFinalPrice()
        );

        cancelledEvent.setReason("ORDER_CANCELLED");

        kafkaService.sendMessage(
                cancelledEvent,
                "order.cancelled",
                savedOrder.getOrderNumber()
        );

        return "Order Cancelled Successfully";
    }

    public void revertInventory(Orders orders) {

        String role = currentUserService.getLoggedInUserRole();

        List<OrderItems> orderItemsList = orders.getOrderItemsList();

        if (orderItemsList.isEmpty()) {

            log.warn("No order items found while reverting inventory");

            throw new OrderItemsNotFoundException("No Order Items Found");
        }

        for (OrderItems orderItems : orderItemsList) {
            long productId = orderItems.getProductId();

            if (productId <= 0) {

                log.warn("Product: {} not found while reverting inventory", productId);

                throw new IllegalArgumentException("No Product Found");
            }

            orderServiceClient.revertInventory(orders.getUsername(), productId, orderItems.getQuantity(), role);

            log.info("Inventory reverted for productId: {}, RestoredQuantity: {}", productId, orderItems.getQuantity());
        }
    }

    protected Orders getOrder(String orderNumber) {
        Orders orders = orderServiceRepository.findByOrderNumber(orderNumber);

        if (orders == null) {

            log.warn("Order not found. OrderNumber: {}", orderNumber);

            throw new OrderNotFoundException("No Order Found");
        }

        return orders;
    }


    private OrdersResDto buildOrderResDto(Orders savedOrder) {
        OrdersResDto ordersResDto = new OrdersResDto();
        ordersResDto.setOrderNumber(savedOrder.getOrderNumber());
        ordersResDto.setOrderStatus(savedOrder.getOrderStatus());
        ordersResDto.setOrderDate(savedOrder.getOrderDate().withNano(0));
        ordersResDto.setOrderItemsResponse(
                savedOrder.getOrderItemsList().stream()
                        .map(item -> new OrderItemsResponseDto(
                                item.getQuantity(),
                                item.getSellingPrice(),
                                item.getDiscount(),
                                item.getTotalPrice(),
                                item.getProductName()
                        ))
                        .toList()
        );
        ordersResDto.setFinalPrice(savedOrder.getFinalPrice());
        ordersResDto.setAppliedCoupon(savedOrder.getAppliedCoupon());
        ordersResDto.setPaymentIntentId(savedOrder.getStripePaymentIntentId());
        return ordersResDto;
    }

    private AdminOrdersResDto buildOrderResDtoForAdmin(Orders savedOrder) {
        AdminOrdersResDto ordersResDto = new AdminOrdersResDto();
        ordersResDto.setUsername(savedOrder.getUsername());
        ordersResDto.setOrderNumber(savedOrder.getOrderNumber());
        ordersResDto.setOrderStatus(savedOrder.getOrderStatus());
        ordersResDto.setOrderDate(savedOrder.getOrderDate().withNano(0));
        ordersResDto.setOrderItemsResponse(
                savedOrder.getOrderItemsList().stream()
                        .map(item -> new OrderItemsResponseDto(
                                item.getQuantity(),
                                item.getSellingPrice(),
                                item.getDiscount(),
                                item.getTotalPrice(),
                                item.getProductName()
                        ))
                        .toList()
        );
        ordersResDto.setFinalPrice(savedOrder.getFinalPrice());
        ordersResDto.setAppliedCoupon(savedOrder.getAppliedCoupon());
        ordersResDto.setPaymentIntentId(savedOrder.getStripePaymentIntentId());
        return ordersResDto;
    }

    @Transactional
    public void updatingStocks(String username, List<OrderItems> orderItemsList, String role, String orderNumber) throws InventoryUpdateFailedException {

        Map<Long, Integer> inventoryUpdate = new HashMap<>();

        for (OrderItems orderItems : orderItemsList) {
            inventoryUpdate.put(orderItems.getProductId(), orderItems.getQuantity());
        }


        orderServiceClient.updateInventory(username, inventoryUpdate, role);


    }

    public void initiatingRefundOnFailedOrders(String orderNumber, String username, String role, String paymentIntent) {

        log.info("Initiating Re-fund for the failed order : {}", orderNumber);
        orderServiceClient.initiateRefund(username, role, paymentIntent);

    }

    private List<OrderItems> buildOrderItems(String username, PlaceOrderRequest placeOrderRequest, Orders order, int discount, double deliveryCharge, String role) throws ProductOutOfStockException {

        List<OrderItems> orderItemsList = new ArrayList<>();

        for (OrderItemRequestDto dto : placeOrderRequest.getItems()) {

            ProductResDto product = orderServiceClient.getProductDetails(username, dto.getProductId(), role);//Need to Get the Product Details From RestTemplate

            int inventoryStock = orderServiceClient.getInventory(dto.getProductId(), username, role).getAvailableQuantity();

            validateIfProductIsAvailableForOrder(inventoryStock, dto.getQuantity());

            double discountForProduct = (discount > 0) ? dto.getQuantity() * calculateOfferPrice(discount, product.getPrice()) + deliveryCharge : dto.getQuantity() * product.getPrice() + deliveryCharge;

            OrderItems item = new OrderItems(
                    product.getName(),
                    product.getProductId(),
                    order,
                    dto.getQuantity(),
                    product.getPrice(),
                    DEFAULT_DISCOUNT,
                    discountForProduct,
                    deliveryCharge
            );

            log.info("Order item added. ProductId: {}, Quantity: {}", product.getProductId(), dto.getQuantity());

            orderItemsList.add(item);
        }

        return orderItemsList;
    }

    private void validateIfProductIsAvailableForOrder(int stockQuantity, int buyQuantity) throws ProductOutOfStockException {

        if (stockQuantity <= 0 || buyQuantity > stockQuantity) {

            log.warn("Product out of stock. AvailableStock: {}, RequestedQuantity: {}", stockQuantity, buyQuantity);

            throw new ProductOutOfStockException("Product is out of stock");
        }
    }

    private int calculateInventory(int stockQuantity, int buyQuantity) {
        return stockQuantity - buyQuantity;
    }

    private double calculateOfferPrice(int discount, double sellingPrice) {
        return sellingPrice * (1 - (discount / 100.0));
    }


    /*private PlaceOrderRequest createOrderItems(List<OrderItemRequestDto> orderItemRequestDtoList) {
        PlaceOrderRequest placeOrderRequest = new PlaceOrderRequest();
        placeOrderRequest.setItems(orderItemRequestDtoList);
        return placeOrderRequest;
    }*/
}
