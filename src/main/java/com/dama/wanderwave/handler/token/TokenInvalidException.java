package com.dama.wanderwave.handler.token;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class TokenInvalidException extends RuntimeException {
    private final String message;
}
