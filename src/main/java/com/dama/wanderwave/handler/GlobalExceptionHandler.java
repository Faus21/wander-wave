package com.dama.wanderwave.handler;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler({Exception.class, Error.class})
	public ResponseEntity<ErrorResponse> handleAllThrowables(Throwable throwable) {
		return buildErrorResponse(throwable);
	}

	private ResponseEntity<ErrorResponse> buildErrorResponse(Throwable throwable) {
		HttpStatus status = ExceptionStatus.getStatusFor(throwable);
		String message = throwable.getMessage();

		if (throwable instanceof MethodArgumentNotValidException ex) {
			message = ex.getBindingResult().getFieldErrors().stream()
					          .map(error -> error.getField() + ": " + error.getDefaultMessage())
					          .collect(Collectors.joining(", ", "Validation failed: ", ""));
		} else if (throwable instanceof ConstraintViolationException ex) {
			message = ex.getConstraintViolations().stream()
					          .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
					          .collect(Collectors.joining(", ", "Validation failed: ", ""));
		}

		return new ResponseEntity<>(createErrorResponse(status, message), status);
	}

	private static ErrorResponse createErrorResponse(HttpStatus status, String message) {
		return new ErrorResponse(status.value(), message);
	}

	public record ErrorResponse(int errorCode, String message) { }
}
