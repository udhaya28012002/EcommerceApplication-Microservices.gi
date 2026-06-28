package org.webapp.ecommerce.exception;

public class DiscountNotApplicable extends RuntimeException{
    public DiscountNotApplicable(String message) {
        super(message);
    }
}
