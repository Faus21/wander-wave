package com.dama.wanderwave.handler;

import lombok.*;
@Data
@EqualsAndHashCode(callSuper=false)
public class TokenNotFoundException extends RuntimeException {
    private final String message;
}
