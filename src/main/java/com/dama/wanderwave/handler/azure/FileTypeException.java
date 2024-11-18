package com.dama.wanderwave.handler.azure;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class FileUploadException extends IllegalArgumentException {
    private final String message;

    public FileUploadException(String message) {
        super(message);
        this.message = message;
    }
}
