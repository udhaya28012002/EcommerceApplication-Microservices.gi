package org.webapp.ecommerce.exception;

public class ContactNoAlreadyExistsException extends ResourceAlreadyExistsException{
    public ContactNoAlreadyExistsException(String message) {
        super(message);
    }
}
