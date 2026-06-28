package org.webapp.ecommerce.exception;

public class OrderStatusUpdateException extends RuntimeException {

    public OrderStatusUpdateException(String message) {
        super(message);
    }
}