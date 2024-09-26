package com.dama.wanderwave.handler;

public class BannedUserException extends RuntimeException {
  public BannedUserException(String message) {
    super(message);
  }
}
