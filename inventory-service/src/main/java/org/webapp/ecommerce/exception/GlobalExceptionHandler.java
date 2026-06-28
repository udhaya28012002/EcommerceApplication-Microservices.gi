package org.webapp.ecommerce.exception;

import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.webapp.ecommerce.dto.errorresponse.NestedErrorResponse;
import org.webapp.ecommerce.dto.errorresponse.ServiceErrorResponse;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            StaleObjectStateException.class,
            StaleStateException.class
    })
    public ResponseEntity<?> handleOptimisticLockingException(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ex.getClass());
    }

    @ExceptionHandler(InvalidInventoryException.class)
    public ResponseEntity<?> handleInvalidInventoryException(InvalidInventoryException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_ACCEPTABLE.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<?> handleInventoryNotFoundException(InventoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
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