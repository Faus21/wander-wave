package com.dama.wanderwave.handler;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PostNotFoundException extends RuntimeException {
  private final String message;
}