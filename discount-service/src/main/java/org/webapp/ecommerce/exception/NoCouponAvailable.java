package org.webapp.ecommerce.exception;

public class NoCouponAvailable extends RuntimeException {
    public NoCouponAvailable(String message) {
        super(message);
    }
}
