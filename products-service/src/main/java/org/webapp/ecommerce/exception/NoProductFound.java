package org.webapp.ecommerce.exception;

public class NoProductFound extends RuntimeException{
    public NoProductFound(String message) {
        super(message);
    }
}
