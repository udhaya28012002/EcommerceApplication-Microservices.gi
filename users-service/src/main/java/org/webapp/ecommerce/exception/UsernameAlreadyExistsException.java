package org.webapp.ecommerce.exception;

public class UsernameAlreadyExistsException extends ResourceAlreadyExistsException{
    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
