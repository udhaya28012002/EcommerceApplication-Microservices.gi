package org.webapp.ecommerce.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.dto.kafkadto.OrderEvent;
import org.webapp.ecommerce.dto.request.PlaceOrderRequest;
import org.webapp.ecommerce.dto.response.PaymentResponse;
import org.webapp.ecommerce.service.OrderRetryService;
import org.webapp.ecommerce.service.OrderService;
import org.webapp.ecommerce.service.OrderStatusService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRetryService orderRetryService;
    private final OrderStatusService orderStatusService;

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    public OrderController(OrderService orderService, OrderRetryService orderRetryService, OrderStatusService orderStatusService) {
        this.orderService = orderService;
        this.orderRetryService = orderRetryService;
        this.orderStatusService = orderStatusService;
    }

    @PostMapping("/placeOrder")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody PlaceOrderRequest placeOrderRequest){

        log.debug("Place order request received");

        return ResponseEntity.status(HttpStatus.CREATED).body(orderRetryService.placeOrderRetry(placeOrderRequest));
    }

    @PatchMapping("/internal/confirmed")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> updateOrderUponPaymentConfirmed(@RequestBody PaymentResponse paymentResponse){

        log.info("Upon Payment confirmed, updating order status and updating Inventory for OrderNumber: {}", paymentResponse.getOrderId());

        OrderEvent res = orderService.confirmOrder(paymentResponse);

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/getOrder/{orderNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getOrderByOrderNumber(@PathVariable String orderNumber){

        log.info("Fetching order details. OrderNumber: {}", orderNumber);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.getOrderByOrderNumber(orderNumber));
    }

    @GetMapping("/getOrders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getOrders(){

        log.info("Fetching logged-in user orders");

        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.getUserOrdersByUserName());
    }

    /*@GetMapping("/getOrders/emailId/{emailId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getOrdersByEmailId(@PathVariable String emailId){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.getUserOrdersByEmailId(emailId));
    }

    @GetMapping("/getOrders/contactNo/{contactNo}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getOrdersByContactNo(@PathVariable String contactNo){
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.getUserOrdersByContactNo(contactNo));
    }*/

    @GetMapping("/getAllOrders")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllOrders(){

        log.info("Fetching all orders");

        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.getAllOrders());
    }

    @PatchMapping("/updateOrder/pending/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderToPending(@PathVariable String orderNumber){

        log.info("Updating order status to PENDING. OrderNumber: {}", orderNumber);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderStatusService.markOrderAsPending(orderNumber));
    }

    @PatchMapping("/updateOrder/confirmed/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderToConfirmed(@PathVariable String orderNumber){

        log.info("Updating order status to CONFIRMED. OrderNumber: {}", orderNumber);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderStatusService.markOrderAsConfirmed(orderNumber));
    }

    @PatchMapping("/updateOrder/processing/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderToProcessing(@PathVariable String orderNumber){

        log.info("Updating order status to PROCESSING. OrderNumber: {}", orderNumber);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderStatusService.markOrderAsProcessing(orderNumber));
    }

    @PatchMapping("/updateOrder/shipped/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderToShipped(@PathVariable String orderNumber){

        log.info("Updating order status to SHIPPED. OrderNumber: {}", orderNumber);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderStatusService.markAsShipped(orderNumber));
    }

    @PatchMapping("/updateOrder/outForDelivery/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderToOutForDelivery(@PathVariable String orderNumber){

        log.info("Updating order status to OUT_FOR_DELIVERY. OrderNumber: {}", orderNumber);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderStatusService.markOrderAsOutForDelivery(orderNumber));
    }

    @PatchMapping("/updateOrder/delivered/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateOrderToDelivered(@PathVariable String orderNumber){

        log.info("Updating order status to DELIVERED. OrderNumber: {}", orderNumber);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderStatusService.markOrderAsDelivered(orderNumber));
    }

    @PatchMapping("/internal/filterUsernameByOrderAmt/{filterAmount}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<?> getUsernamesByFilterAmt(@PathVariable long filterAmount){
        return ResponseEntity.ok(orderService.filterUsernamesByOrderAmt(filterAmount));
    }

    @PatchMapping("/cancelOrder/{orderNumber}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderNumber){

        log.debug("Cancel order request received. OrderNumber: {}", orderNumber);

        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.cancelOrder(orderNumber));
    }
}
