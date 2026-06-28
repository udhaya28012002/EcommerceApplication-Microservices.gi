package org.webapp.ecommerce.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.client.OrderServiceClient;
import org.webapp.ecommerce.dto.request.OrderItemRequestDto;
import org.webapp.ecommerce.dto.request.PlaceOrderRequest;
import org.webapp.ecommerce.dto.response.*;
import org.webapp.ecommerce.entity.OrderItems;
import org.webapp.ecommerce.entity.OrderStatus;
import org.webapp.ecommerce.entity.Orders;
import org.webapp.ecommerce.exception.OrderItemsNotFoundException;
import org.webapp.ecommerce.exception.OrderNotFoundException;
import org.webapp.ecommerce.exception.OrderStatusUpdateException;
import org.webapp.ecommerce.exception.ProductOutOfStockException;
import org.webapp.ecommerce.repository.OrderRepository;
import org.webapp.ecommerce.util.CurrentUserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderServiceRepository;
    private final CurrentUserService currentUserService;
    private final OrderServiceClient orderServiceClient;

    private static final int DEFAULT_DISCOUNT = 0;

    @PersistenceContext
    private EntityManager entityManager;

    private final Logger log = LoggerFactory.getLogger(OrderService.class);


    public static final Map<OrderStatus, Set<OrderStatus>> VALID_STATUS_TRANSITIONS =
            Map.of(
                    OrderStatus.CREATED,
                    Set.of(OrderStatus.PENDING, OrderStatus.CANCELLED),

                    OrderStatus.PENDING,
                    Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),

                    OrderStatus.CONFIRMED,
                    Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),

                    OrderStatus.PROCESSING,
                    Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),

                    OrderStatus.SHIPPED,
                    Set.of(OrderStatus.OUT_OF_DELIVERY),

                    OrderStatus.OUT_OF_DELIVERY,
                    Set.of(OrderStatus.DELIVERED),

                    OrderStatus.DELIVERED,
                    Set.of(),

                    OrderStatus.CANCELLED,
                    Set.of()
            );

    public OrderService(OrderRepository orderServiceRepository, CurrentUserService currentUserService, OrderServiceClient orderServiceClient) {
        this.orderServiceRepository = orderServiceRepository;
        this.currentUserService = currentUserService;
        this.orderServiceClient = orderServiceClient;
    }

    @Transactional
    public OrdersResDto placeOrder(PlaceOrderRequest placeOrderRequest) throws OrderItemsNotFoundException {

        int discount = 0;

        double deliveryCharge = 0;

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Order placement started for user: {}", loggedUser);

       if (placeOrderRequest.getItems() == null || placeOrderRequest.getItems().isEmpty()) {

            log.warn("Order items missing for user: {}", loggedUser);

            throw new OrderItemsNotFoundException("Order info is missing");
        }

        LocalDateTime today = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        Orders order = new Orders();
        order.setOrderDate(today);
        order.setUsername(loggedUser);
        order.setOrderNumber("ORD-" + today.format(formatter));

        log.info("Generated order number: {}", order.getOrderNumber());

        List<OrderItems> orderItemsList = buildOrderItems(loggedUser,placeOrderRequest, order, discount, deliveryCharge, role);

        double totalPricePerOrder = orderItemsList.stream()
                .mapToDouble(OrderItems::getTotalPrice)
                .sum();

        log.info("Total order price before discount: {}", totalPricePerOrder);

        ApplyCouponResponse applyCouponResponse = orderServiceClient.checkCouponsAndRedeem(loggedUser, placeOrderRequest.getCouponCode(), totalPricePerOrder, role);// Need to Invoke From Discount Service:

        double finalPrice = applyCouponResponse.getFinalPrice();
        order.setFinalPrice(finalPrice);

        order.setAppliedCoupon(applyCouponResponse.getCouponName());

        order.setOrderItemsList(orderItemsList);

        log.info("Final order price after discount: {}", finalPrice);

        // PAYMENT PAGE (IF SUCCESS WE NEED TO PLACE ORDER OTHERWISE REVERT),

        updatingStocks(loggedUser, orderItemsList, role);

        log.info("Inventory updated successfully for order: {}", order.getOrderNumber());

        order.setOrderStatus(OrderStatus.CONFIRMED);

        Orders savedOrder = orderServiceRepository.save(order);

        if(savedOrder.getOrderStatus().equals(OrderStatus.CONFIRMED)){
            log.info("Order confirmed successfully for user: {}. OrderId: {}",loggedUser, savedOrder.getOrderId());
        }

        return buildOrderResDto(savedOrder);
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

        for(CartCategoryResponseDto cartCategory: cart.getCartItemsCategoryResponseDtoList()){
            prepareOrderReq.setItems(prepareOrderFromCart(cartCategory.getCartItemsResponseDtoList()));
        }

        placeOrder(prepareOrderReq);

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

    @Transactional
    private void updateOrderStatus(String orderNumber, OrderStatus newOrderStatus) {

        log.debug("Updating order status. OrderNumber: {}, NewStatus: {}", orderNumber, newOrderStatus);

        Orders orders = getOrder(orderNumber);

        validateOrderStatusUpdate(orders.getOrderStatus(), newOrderStatus);

        orders.setOrderStatus(newOrderStatus);
        orderServiceRepository.save(orders);

        log.info("Order status updated successfully. OrderNumber: {}, Status: {}", orderNumber, newOrderStatus);
    }

    public String markOrderAsPending(String orderNumber) {
        updateOrderStatus(orderNumber, OrderStatus.PENDING);
        return "Status Changed";
    }

    public String markOrderAsConfirmed(String orderNumber) {
        updateOrderStatus(orderNumber, OrderStatus.CONFIRMED);
        return "Status Changed";
    }

    public String markOrderAsProcessing(String orderNumber) {
        updateOrderStatus(orderNumber, OrderStatus.PROCESSING);
        return "Status Changed";
    }

    public String markAsShipped(String orderNumber) {
        updateOrderStatus(orderNumber, OrderStatus.SHIPPED);
        return "Status Changed";
    }

    public String markOrderAsOutForDelivery(String orderNumber) {
        updateOrderStatus(orderNumber, OrderStatus.OUT_OF_DELIVERY);
        return "Status Changed";
    }

    public String markOrderAsDelivered(String orderNumber) {
        updateOrderStatus(orderNumber, OrderStatus.DELIVERED);
        return "Status Changed";
    }

    public List<String> filterUsernamesByOrderAmt(double orderAmt){
        return orderServiceRepository.findUsernamesByFilterPrice(orderAmt);
    }

    @Transactional
    public String cancelOrder(String orderNumber) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Cancel order request initiated. User: {}, OrderNumber: {}", loggedUser, orderNumber);

        Orders orders = getOrder(orderNumber);

        validateOrderStatusUpdate(orders.getOrderStatus(), OrderStatus.CANCELLED);

        //Rever the Inventory Count
        revertInventory(orders);

        //Revert the Coupons if any used:

        orderServiceClient.revertCoupon(loggedUser, orders.getAppliedCoupon(), role);

        log.info("Inventory reverted successfully for cancelled order: {}", orderNumber);

        //Updating the Order Status
        orders.setOrderStatus(OrderStatus.CANCELLED);

        orderServiceRepository.save(orders);

        log.info("Order cancelled successfully. OrderNumber: {}", orderNumber);

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

            if (productId > 0) {

                log.warn("Product not found while reverting inventory");

                throw new IllegalArgumentException("No Product Found");
            }

            orderServiceClient.revertInventory(orders.getUsername(), productId, orderItems.getQuantity(), role);

            log.info("Inventory reverted for productId: {}, RestoredQuantity: {}", productId, orderItems.getQuantity());
        }
    }

    private Orders getOrder(String orderNumber) {
        Orders orders = orderServiceRepository.findByOrderNumber(orderNumber);

        if (orders == null) {

            log.warn("Order not found. OrderNumber: {}", orderNumber);

            throw new OrderNotFoundException("No Order Found");
        }

        return orders;
    }

    private void validateOrderStatusUpdate(OrderStatus prevOrderStatus,
                                           OrderStatus newOrderStatus) {

        // Prevent same status update
        if (prevOrderStatus == newOrderStatus) {

            log.warn("Duplicate order status update attempted. Status: {}", newOrderStatus);

            throw new OrderStatusUpdateException("Order is already in " + newOrderStatus + " status");
        }

        Set<OrderStatus> allowedStatuses = VALID_STATUS_TRANSITIONS.get(prevOrderStatus);

        if (!allowedStatuses.contains(newOrderStatus)) {

            log.warn("Invalid order status transition attempted. From: {}, To: {}", prevOrderStatus, newOrderStatus);

            throw new OrderStatusUpdateException("Invalid status transition from " + prevOrderStatus + " to " + newOrderStatus);
        }
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
        return ordersResDto;
    }

    public void updatingStocks(String username, List<OrderItems> orderItemsList, String role) {

        Map<Long, Integer> inventoryUpdate = new HashMap<>();

        for (OrderItems orderItems : orderItemsList) {

            inventoryUpdate.put(orderItems.getProductId(), orderItems.getQuantity());

            entityManager.flush();
        }

        orderServiceClient.updateInventory(username, inventoryUpdate, role);

    }

    private List<OrderItems> buildOrderItems(String username, PlaceOrderRequest placeOrderRequest, Orders order, int discount, double deliveryCharge, String role) throws ProductOutOfStockException{

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

    private void validateIfProductIsAvailableForOrder(int stockQuantity, int buyQuantity) throws ProductOutOfStockException{

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
