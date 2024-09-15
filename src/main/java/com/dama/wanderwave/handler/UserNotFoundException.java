package com.dama.wanderwave.handler;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class UserNotFoundException extends RuntimeException {
    private final String message;
}
