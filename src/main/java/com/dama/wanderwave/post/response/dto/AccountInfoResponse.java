package com.dama.wanderwave.post.response.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountInfoResponse {

    private String nickname;
    private String imageUrl;

}
