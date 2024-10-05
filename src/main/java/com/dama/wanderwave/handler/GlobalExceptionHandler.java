package com.dama.wanderwave.handler;

import jakarta.validation.ConstraintViolationException;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ControllerAdvice
public class GlobalExceptionHandler {

	private static final String VALIDATION_FAILED_FORMAT = "Validation failed: %s";

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<ErrorResponse> handleAllThrowables(Throwable throwable) {
		Throwable rootCause = getRootCause(throwable);
		return buildErrorResponse(rootCause);
	}

	private ResponseEntity<ErrorResponse> buildErrorResponse(Throwable throwable) {
		HttpStatus status = ExceptionStatus.getStatusFor(throwable);
		String message = resolveErrorMessage(throwable);
		return ResponseEntity.status(status).body(ErrorResponse.builder()
				                                          .errorCode(status.value())
				                                          .message(message)
				                                          .build());
	}

	private String resolveErrorMessage(Throwable throwable) {
		if (throwable instanceof MethodArgumentNotValidException ex) {
			return formatValidationErrors(
					ex.getBindingResult().getFieldErrors(),
					error -> formatErrorMessage(error.getField(), error.getDefaultMessage())
			);
		} else if (throwable instanceof ConstraintViolationException ex) {
			return formatValidationErrors(
					ex.getConstraintViolations(),
					violation -> formatErrorMessage(violation.getPropertyPath().toString(), violation.getMessage())
			);
		}
		return throwable.getMessage();
	}

	private String formatErrorMessage(String field, String message) {
		return field + ": " + message;
	}

	private <T> String formatValidationErrors(Iterable<T> errors, Function<T, String> mapper) {
		String errorMessages = StreamSupport.stream(errors.spliterator(), false)
				                       .map(mapper)
				                       .collect(Collectors.joining(", "));
		return String.format(VALIDATION_FAILED_FORMAT, errorMessages);
	}

	private Throwable getRootCause(Throwable throwable) {
		while (throwable.getCause() != null && throwable.getCause() != throwable) {
			throwable = throwable.getCause();
		}
		return throwable;
	}

	@Builder
	public record ErrorResponse(int errorCode, String message) {}
}