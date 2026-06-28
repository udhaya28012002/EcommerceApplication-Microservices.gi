package org.webapp.ecommerce.exception;

import jakarta.validation.ConstraintViolationException;
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

    @ExceptionHandler(NoProductFound.class)
    public ResponseEntity<?> handleProductNotFound(NoProductFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(ProductOutOfStockException.class)
    public ResponseEntity<?> handleProductOutOfStock(ProductOutOfStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ServiceErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<?> handleCategoryAlreadyExists(CategoryAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ServiceErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<?> handleCategoryNotFound(CategoryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NestedErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {

        System.out.println("This is being Invoked.....");

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

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<NestedErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

        NestedErrorResponse nestedErrorResponse = new NestedErrorResponse();

        ex.getConstraintViolations().forEach(violation -> {
            ServiceErrorResponse errorResponse = new ServiceErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString(),
                    violation.getMessage()   // your custom message from DTO annotation
            );
            nestedErrorResponse.getListOfErrors().add(errorResponse);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(nestedErrorResponse);
    }

}