package org.webapp.ecommerce.exception;

public class UnknowPaymentStatusException extends RuntimeException {
    public UnknowPaymentStatusException(String exe) {
        super(exe);
    }
}
