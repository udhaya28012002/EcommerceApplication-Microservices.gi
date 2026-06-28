package org.webapp.ecommerce.exception;

public class PasswordReuseException extends RuntimeException {
    public PasswordReuseException(String s) {
        super(s);
    }
}
