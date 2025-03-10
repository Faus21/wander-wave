package com.dama.wanderwave.handler.chat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class ChatRoomException extends IllegalArgumentException {
    public final String message;
}
