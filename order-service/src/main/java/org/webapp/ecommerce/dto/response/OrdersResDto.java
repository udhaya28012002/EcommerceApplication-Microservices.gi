package org.webapp.ecommerce.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.webapp.ecommerce.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class OrdersResDto {

    @NotBlank
    private String orderNumber;

    @NotNull
    private OrderStatus orderStatus;

    private List<OrderItemsResponseDto> orderItemsResponse;

    private String appliedCoupon;

    private double finalPrice;

    private LocalDateTime orderDate;

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
}