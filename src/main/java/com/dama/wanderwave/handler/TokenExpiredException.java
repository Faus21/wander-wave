package com.dama.wanderwave.handler;

import lombok.*;
@Data
@EqualsAndHashCode(callSuper=false)
public class TokenExpiredException extends RuntimeException {
    private final String message;
}
