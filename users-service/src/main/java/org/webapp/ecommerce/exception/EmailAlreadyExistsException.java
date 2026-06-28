package org.webapp.ecommerce.exception;

public class EmailAlreadyExistsException extends ResourceAlreadyExistsException{
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
