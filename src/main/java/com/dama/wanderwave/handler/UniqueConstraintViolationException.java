package com.dama.wanderwave.handler;

import lombok.Getter;

@Getter
public class UniqueConstraintViolationException extends RuntimeException {
    private final String errorCode;
    private final String fieldName;

    public UniqueConstraintViolationException(String message, String errorCode, String fieldName) {
        super(message);
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

    public UniqueConstraintViolationException(String message, String errorCode, String fieldName, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.fieldName = fieldName;
    }

}
