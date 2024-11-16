package com.dama.wanderwave.handler.user.save;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class IsSavedException extends RuntimeException {
    private final String message;
}