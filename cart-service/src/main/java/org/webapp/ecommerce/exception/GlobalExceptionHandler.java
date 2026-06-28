package org.webapp.ecommerce.exception;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.webapp.ecommerce.dto.errorresponse.NestedErrorResponse;
import org.webapp.ecommerce.dto.errorresponse.ServiceErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CartEmptyException.class)
    public ResponseEntity<?> cartEmptyException(CartEmptyException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ServiceErrorResponse(HttpStatus.BAD_REQUEST.value(), LocalDateTime.now().toString(), ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ServiceErrorResponse(HttpStatus.UNAUTHORIZED.value(), LocalDateTime.now().toString(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ServiceErrorResponse(HttpStatus.FORBIDDEN.value(), LocalDateTime.now().toString(), ex.getMessage()));
    }

    @ExceptionHandler(InvalidCartException.class)
    public ResponseEntity<?> handleInvalidCartException(InvalidCartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ServiceErrorResponse(HttpStatus.BAD_REQUEST.value(), LocalDateTime.now().toString(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NestedErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {

        NestedErrorResponse nestedErrorResponse = new NestedErrorResponse();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
                    ServiceErrorResponse errorResponse = new ServiceErrorResponse(HttpStatus.BAD_REQUEST.value(),
                            LocalDateTime.now().toString(),
                            error.getDefaultMessage());
                    nestedErrorResponse.getListOfErrors().add(errorResponse);
                }
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(nestedErrorResponse);
    }
}