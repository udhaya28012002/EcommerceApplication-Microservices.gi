package org.webapp.ecommerce.exception;

public class UserAccessDeniedException extends RuntimeException{
    public UserAccessDeniedException(String message){
        super(message);
    }
}
