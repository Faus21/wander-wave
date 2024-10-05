package com.dama.wanderwave.auth;

import lombok.Builder;
import org.springframework.http.HttpStatus;

@Builder
public record ResponseRecord (int code, Object message) {
}
