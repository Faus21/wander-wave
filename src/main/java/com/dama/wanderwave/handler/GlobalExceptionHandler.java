package com.dama.wanderwave.handler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), 404, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFoundException(RoleNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), 404, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        String message = "Validation failed for fields: " + fieldErrors;
        return buildErrorResponse(message, 400, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> violations = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            violations.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        String message = "Validation failed for parameters: " + violations;
        return buildErrorResponse(message, 401, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalidException(TokenInvalidException ex) {
        return buildErrorResponse(ex.getMessage(), 400, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException ex) {
        return buildErrorResponse(ex.getMessage(), 400, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException ex) {
        return buildErrorResponse(ex.getMessage(), 400, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTokenNotFoundException(TokenNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), 404, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        return buildErrorResponse(ex.getMessage(), 401, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UniqueConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleUniqueConstraintViolationException(UniqueConstraintViolationException ex) {
        return buildErrorResponse(ex.getMessage(), 409, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmailTemplateException.class)
    public ResponseEntity<ErrorResponse> handleEmailTemplateException(EmailTemplateException ex) {
        return buildErrorResponse(ex.getMessage(), 500, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(EmailSendingException.class)
    public ResponseEntity<ErrorResponse> handleEmailSendingException(EmailSendingException ex) {
        return buildErrorResponse(ex.getMessage(), 500, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse(ex.getMessage(), 500, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InternalError.class)
    public ResponseEntity<ErrorResponse> handleInternalError(InternalError ex) {
        return buildErrorResponse(ex.getMessage(), 500, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, int errorCode, HttpStatus status) {
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        return new ResponseEntity<>(errorResponse, status);
    }

    public record ErrorResponse(int errorCode, String message) { }
}