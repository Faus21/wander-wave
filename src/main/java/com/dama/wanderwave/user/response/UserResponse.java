package com.dama.wanderwave.user.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String id;
    private String nickname;
    private String email;
    private String description;
    private int subscriberCount;
    private int subscriptionsCount;
    private String avatarUrl;
}
