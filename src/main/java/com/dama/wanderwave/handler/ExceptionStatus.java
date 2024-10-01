package com.dama.wanderwave.handler;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Arrays;

public enum ExceptionStatus {
    USER_NOT_FOUND(UserNotFoundException.class, HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(RoleNotFoundException.class, HttpStatus.NOT_FOUND),
    METHOD_ARGUMENT_NOT_VALID(MethodArgumentNotValidException.class, HttpStatus.BAD_REQUEST),
    CONSTRAINT_VIOLATION(ConstraintViolationException.class, HttpStatus.BAD_REQUEST),
    TOKEN_INVALID(TokenInvalidException.class, HttpStatus.BAD_REQUEST),
    TOKEN_REFRESH(TokenRefreshException.class, HttpStatus.BAD_REQUEST),
    TOKEN_EXPIRED(TokenExpiredException.class, HttpStatus.BAD_REQUEST),
    TOKEN_NOT_FOUND(TokenNotFoundException.class, HttpStatus.NOT_FOUND),
    CHAT_ROOM_NOT_FOUND(ChatRoomNotFoundException.class, HttpStatus.NOT_FOUND),
    BAD_CREDENTIALS(BadCredentialsException.class, HttpStatus.UNAUTHORIZED),
    UNIQUE_CONSTRAINT_VIOLATION(UniqueConstraintViolationException.class, HttpStatus.CONFLICT),
    EMAIL_TEMPLATE(EmailTemplateException.class, HttpStatus.INTERNAL_SERVER_ERROR),
    EMAIL_SENDING(EmailSendingException.class, HttpStatus.INTERNAL_SERVER_ERROR),
    GENERIC_EXCEPTION(Exception.class, HttpStatus.INTERNAL_SERVER_ERROR),
    INTERNAL_ERROR(InternalError.class, HttpStatus.INTERNAL_SERVER_ERROR);

    private final Class<? extends Throwable> exceptionClass;
    private final HttpStatus status;

    ExceptionStatus(Class<? extends Throwable> exceptionClass, HttpStatus status) {
        this.exceptionClass = exceptionClass;
        this.status = status;
    }

    public static HttpStatus getStatusFor(Throwable throwable) {
        return Arrays.stream(values())
                .filter(e -> e.exceptionClass.isAssignableFrom(throwable.getClass()))
                .map(e -> e.status)
                .findFirst()
                .orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
