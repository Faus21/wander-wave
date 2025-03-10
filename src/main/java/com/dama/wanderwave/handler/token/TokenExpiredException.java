package com.dama.wanderwave.handler.token;

import lombok.*;
@Data
@EqualsAndHashCode(callSuper=false)
public class TokenExpiredException extends RuntimeException {
    private final String message;
}
