package com.dama.wanderwave.handler;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), "USER_NOT_FOUND", HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFoundException(RoleNotFoundException ex) {
        return buildErrorResponse(ex.getMessage(), "ROLE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(TokenInvalidException.class)
    public ResponseEntity<ErrorResponse> handleTokenInvalidException(TokenInvalidException ex) {
        return buildErrorResponse(ex.getMessage(), "TOKEN_INVALID", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ErrorResponse> handleTokenExpiredException(TokenExpiredException ex) {
        return buildErrorResponse(ex.getMessage(), "TOKEN_EXPIRED", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return buildErrorResponse("An unexpected error occurred", "INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(String message, String errorCode, HttpStatus status) {
        ErrorResponse errorResponse = new ErrorResponse(message, errorCode);
        return new ResponseEntity<>(errorResponse, status);
    }

    public record ErrorResponse(String message, String errorCode) { }
}
