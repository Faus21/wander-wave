package com.dama.wanderwave.user.response;

import com.dama.wanderwave.user.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShortUserResponse {
    private String id;
    private String nickname;
    private String email;
    private String avatarUrl;

    public static ShortUserResponse fromEntity(User user) {
        return ShortUserResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .avatarUrl(user.getImageUrl())
                .build();
    }
}
