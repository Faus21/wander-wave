package com.dama.wanderwave.handler.token;

import lombok.Data;
import lombok.EqualsAndHashCode;
@Data
@EqualsAndHashCode(callSuper=false)
public class TokenRefreshException extends RuntimeException {
	private final String message;
}
