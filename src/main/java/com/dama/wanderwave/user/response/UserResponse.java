package com.dama.wanderwave.user.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String nickname;
    private String email;
    private String description;
    private int subscriberCount;
    private int subscriptionsCount;
    private String avatarUrl;
}
