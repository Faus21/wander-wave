package com.dama.wanderwave.message.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private String id;
    private String chatId;
    private String senderId;
    private String recipientId;
    private String content;
    private LocalDateTime createdAt;
}