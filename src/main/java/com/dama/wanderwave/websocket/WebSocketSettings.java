package com.dama.wanderwave.websocket;

import lombok.Getter;

@Getter
public enum WebSocketSettings {
    CHAT_ENDPOINT("/chat"),
    NOTIFICATION_ENDPOINT("/notifications"),
    ALLOWED_ORIGINS("*"),
    HEARTBEAT_TIME(25000),
    STREAM_BYTES_LIMIT(512 * 1024),
    HTTP_MESSAGE_CACHE_SIZE(1000),
    DISCONNECT_DELAY(30 * 1000),
    MAX_BUFFER_SIZE(8192),
    MESSAGE_SIZE_LIMIT(4 * 8192),
    TIME_TO_FIRST_MESSAGE(30000);

    private final String stringValue;
    private final int intValue;

    WebSocketSettings(String value) {
        this.stringValue = value;
        this.intValue = -1;
    }

    WebSocketSettings(int value) {
        this.intValue = value;
        this.stringValue = null;
    }

    public boolean isString() {
        return stringValue != null;
    }
}
