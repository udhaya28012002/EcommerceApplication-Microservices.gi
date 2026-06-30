package org.webapp.ecommerce.exception;

import com.stripe.exception.StripeException;
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

    // 404 — payment record not found in DB
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ServiceErrorResponse> handlePaymentNotFoundException(
            PaymentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    // 406 — invalid state transition (e.g. SUCCEEDED → INITIATED)
    @ExceptionHandler(PaymentStatusTransitionException.class)
    public ResponseEntity<ServiceErrorResponse> handlePaymentStatusTransitionException(
            PaymentStatusTransitionException ex) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_ACCEPTABLE.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    // 404 — unknown payment status passed
    @ExceptionHandler(UnknowPaymentStatusException.class)
    public ResponseEntity<ServiceErrorResponse> handleUnknowPaymentStatusException(
            UnknowPaymentStatusException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    // 401 — service token invalid or missing
    @ExceptionHandler(ServiceAuthException.class)
    public ResponseEntity<ServiceErrorResponse> handleServiceAuthException(
            ServiceAuthException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ServiceErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    // 402 — Stripe API errors (test mode failures, invalid params, etc.)
    @ExceptionHandler(StripeException.class)
    public ResponseEntity<ServiceErrorResponse> handleStripeException(
            StripeException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(new ServiceErrorResponse(
                        HttpStatus.PAYMENT_REQUIRED.value(),
                        LocalDateTime.now().toString(),
                        "Stripe error: " + ex.getMessage()
                ));
    }

    // 400 — @Valid annotation failures on request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NestedErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        NestedErrorResponse nestedErrorResponse = new NestedErrorResponse();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            ServiceErrorResponse errorResponse = new ServiceErrorResponse(
                    HttpStatus.BAD_REQUEST.value(),
                    LocalDateTime.now().toString(),   // fix: was LocalDateTime, needs String
                    error.getField() + ": " + error.getDefaultMessage()
            );
            nestedErrorResponse.getListOfErrors().add(errorResponse);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(nestedErrorResponse);
    }

    // 500 — catch-all for anything unexpected
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ServiceErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ServiceErrorResponse(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        LocalDateTime.now().toString(),
                        "An unexpected error occurred: " + ex.getMessage()
                ));
    }
}