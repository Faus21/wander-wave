package com.dama.wanderwave.user.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscribeRequest {

    @NotEmpty(message = "Follower id is mandatory")
    private String followerId;
    @NotEmpty(message = "Followed id is mandatory")
    private String followedId;

}
