package org.webapp.ecommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.webapp.ecommerce.dto.errorresponse.NestedErrorResponse;
import org.webapp.ecommerce.dto.errorresponse.ServiceErrorResponse;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<?> handleOrderNotFound(OrderNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(OrderStatusUpdateException.class)
    public ResponseEntity<?> OrderStatusUpdateException(OrderStatusUpdateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ServiceErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(OrderItemsNotFoundException.class)
    public ResponseEntity<?> OrderItemsNotFound(OrderItemsNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> OrderItemsNotFound(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(OrderProcessingException.class)
    public ResponseEntity<?> OrderItemsNotFound(OrderProcessingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ServiceErrorResponse(
                        HttpStatus.CONFLICT.value(),
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