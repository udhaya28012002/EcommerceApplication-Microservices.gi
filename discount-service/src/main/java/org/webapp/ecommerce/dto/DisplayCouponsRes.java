package org.webapp.ecommerce.dto;

import java.util.Map;

public class DisplayCouponsRes {

    private Map<String, CouponDetailsRes> availableCoupons;

    public Map<String, CouponDetailsRes> getAvailableCoupons() {
        return availableCoupons;
    }

    public void setAvailableCoupons(Map<String, CouponDetailsRes> availableCoupons) {
        this.availableCoupons = availableCoupons;
    }
}
