package com.dama.wanderwave.handler.user.like;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class LikeNotFoundException extends RuntimeException {
  private final String message;
}
