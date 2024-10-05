package com.dama.wanderwave.handler;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ReportNotFoundException extends RuntimeException {
    private final String message;
}
