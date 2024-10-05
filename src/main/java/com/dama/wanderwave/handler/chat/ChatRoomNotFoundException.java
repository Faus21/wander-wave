package com.dama.wanderwave.handler.chat;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class ChatRoomNotFoundException extends RuntimeException {
	public final String message;
}
