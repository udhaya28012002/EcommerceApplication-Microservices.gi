package org.webapp.ecommerce.exception;

public class InvalidInventoryException extends RuntimeException{
    public InvalidInventoryException(String message) {
        super(message);
    }
}
