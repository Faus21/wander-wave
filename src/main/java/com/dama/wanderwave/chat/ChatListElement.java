package com.dama.wanderwave.chat;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatListElement(String userId,
                              String name,
                              String imgUrl,
                              String content,
                              LocalDateTime createdAt) { }
