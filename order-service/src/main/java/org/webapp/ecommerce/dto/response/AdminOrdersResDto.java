package org.webapp.ecommerce.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.webapp.ecommerce.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class AdminOrdersResDto {

    @NotBlank
    private String orderNumber;

    private String username;

    @NotNull
    private OrderStatus orderStatus;

    private List<OrderItemsResponseDto> orderItemsResponse;

    private String appliedCoupon;

    private double finalPrice;

    private LocalDateTime orderDate;

    private String paymentIntentId;

    public String getPaymentIntentId() {
        return paymentIntentId;
    }

    public void setPaymentIntentId(String paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public String getAppliedCoupon() {
        return appliedCoupon;
    }

    public void setAppliedCoupon(String appliedCoupon) {
        this.appliedCoupon = appliedCoupon;
    }


    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }

    public List<OrderItemsResponseDto> getOrderItemsResponse() {
        return orderItemsResponse;
    }

    public void setOrderItemsResponse(List<OrderItemsResponseDto> orderItemsResponse) {
        this.orderItemsResponse = orderItemsResponse;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}