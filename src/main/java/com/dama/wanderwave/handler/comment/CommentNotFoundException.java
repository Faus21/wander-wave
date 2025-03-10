package com.dama.wanderwave.handler.comment;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CommentNotFoundException extends RuntimeException {
    private final String message;
}