package com.dama.wanderwave.handler;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class UnauthorizedActionException extends RuntimeException {
    private final String message;
}
