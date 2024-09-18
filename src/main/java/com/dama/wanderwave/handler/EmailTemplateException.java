package com.dama.wanderwave.handler;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class EmailTemplateException extends RuntimeException {
    public EmailTemplateException(String message) {
        super(message);
    }
}