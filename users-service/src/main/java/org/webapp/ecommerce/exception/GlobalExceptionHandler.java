package org.webapp.ecommerce.exception;

import io.jsonwebtoken.ExpiredJwtException;
import org.webapp.ecommerce.auth.exception.InvalidTokenException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.webapp.ecommerce.dto.errorresponse.NestedErrorResponse;
import org.webapp.ecommerce.dto.errorresponse.ServiceErrorResponse;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> jwtTokenExpired(ExpiredJwtException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ServiceErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> badCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ServiceErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<?> invalidToken(InvalidTokenException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> userNotFound(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ServiceErrorResponse(
                        HttpStatus.NOT_FOUND.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<?> disableAccount(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ServiceErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ServiceErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<?> handleAlreadyExists(ResourceAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ServiceErrorResponse(
                        HttpStatus.CONFLICT.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<?> AddressNotFound(AddressNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ServiceErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ServiceErrorResponse(
                        HttpStatus.UNAUTHORIZED.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(PasswordReuseException.class)
    public ResponseEntity<?> passwordReuseException(PasswordReuseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ServiceErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body("Authentication failed");
    }

    @ExceptionHandler(UserAccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(UserAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ServiceErrorResponse(
                        HttpStatus.FORBIDDEN.value(),
                        LocalDateTime.now().toString(),
                        ex.getMessage()
                ));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<?> handlePasswordMismatch(PasswordMismatchException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ServiceErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
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