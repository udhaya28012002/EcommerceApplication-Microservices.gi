package org.webapp.ecommerce.exception;

public class PaymentStatusTransitionException extends RuntimeException{
    public PaymentStatusTransitionException(String message) {
        super(message);
    }
}
