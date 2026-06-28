package org.webapp.ecommerce.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class PlaceOrderRequest {

    @NotEmpty(message = "Coupon Code is required")
    private String couponCode;

    @NotEmpty(message = "OrderItems List is required")
    private List<OrderItemRequestDto> items;

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public List<OrderItemRequestDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequestDto> items) {
        this.items = items;
    }
}
