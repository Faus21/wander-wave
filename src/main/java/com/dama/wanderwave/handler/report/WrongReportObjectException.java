package com.dama.wanderwave.handler.report;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class WrongReportObjectException extends RuntimeException {
    private final String message;
}