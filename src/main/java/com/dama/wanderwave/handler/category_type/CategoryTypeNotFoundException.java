package com.dama.wanderwave.handler.category_type;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class CategoryTypeNotFoundException extends RuntimeException {
	private final String message;
}
