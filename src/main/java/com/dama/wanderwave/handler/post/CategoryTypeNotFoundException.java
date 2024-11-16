package com.dama.wanderwave.handler.post;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CategoryTypeNotFoundException extends RuntimeException {
    private final String message;
}
