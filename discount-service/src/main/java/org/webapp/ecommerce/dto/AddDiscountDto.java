package org.webapp.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.webapp.ecommerce.entity.DiscountType;

public class AddDiscountDto {

    @NotBlank(message = "Coupon Code is required")
    @NotNull(message = "Coupon Code is required")
    private String couponCode;

    @NotBlank(message = "Coupon Description is required")
    @NotNull(message = "Coupon Description is required")
    private String description;

    @NotNull(message = "DiscountType should be required")
    private DiscountType discountType;

    @Min(value = 1, message = "Discount value should be valid")
    private long discountValue;

    @Min(value = 1, message = "MinOrderAmt should be valid")
    private long minAmtOrder;

    @Min(value = 1, message = "MaxDiscountAmt should be valid")
    private long maxDiscountAmount;

    @Min(value = 1, message = "Validity(In Months) should be valid")
    private int validityInMonths;

    @Min(value = 1, message = "UsageLimit should be valid")
    private int usageLimit;

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public long getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(long discountValue) {
        this.discountValue = discountValue;
    }

    public long getMinAmtOrder() {
        return minAmtOrder;
    }

    public void setMinAmtOrder(long minAmtOrder) {
        this.minAmtOrder = minAmtOrder;
    }

    public long getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public void setMaxDiscountAmount(long maxDiscountAmount) {
        this.maxDiscountAmount = maxDiscountAmount;
    }

    public int getValidityInMonths() {
        return validityInMonths;
    }

    public void setValidityInMonths(int validityInMonths) {
        this.validityInMonths = validityInMonths;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
