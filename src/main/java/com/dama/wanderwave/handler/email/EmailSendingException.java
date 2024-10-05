package com.dama.wanderwave.handler.email;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class EmailSendingException extends RuntimeException {
    private final String message;
}