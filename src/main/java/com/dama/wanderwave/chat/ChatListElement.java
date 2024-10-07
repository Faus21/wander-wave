package com.dama.wanderwave.chat;

import lombok.Builder;

@Builder
public record ChatListElement(String userId,
                              String name,
                              String imgUrl,
                              String content) { }
