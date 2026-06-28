package org.webapp.ecommerce.dto.response;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class ApplyCouponResponse {

    @NotBlank(message = "Coupon Code is required")
    private String couponName;

    private boolean isApplied;

    @NotBlank(message = "Reason should be defined")
    private String message;

    @Min(value = 1, message = "Final Price should be valid")
    private double finalPrice;

    public String getCouponName() {
        return couponName;
    }

    public void setCouponName(String couponName) {
        this.couponName = couponName;
    }

    public boolean isApplied() {
        return isApplied;
    }

    public void setApplied(boolean applied) {
        isApplied = applied;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }
}
