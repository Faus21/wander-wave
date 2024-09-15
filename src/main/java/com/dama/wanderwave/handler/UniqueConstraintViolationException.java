package com.dama.wanderwave.handler;

import lombok.Getter;

@Getter
public class UniqueConstraintViolationException extends RuntimeException {
    private final String fieldName;

    public UniqueConstraintViolationException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

}