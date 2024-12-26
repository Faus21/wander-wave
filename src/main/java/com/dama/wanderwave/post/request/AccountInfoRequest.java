package com.dama.wanderwave.post.request;


import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AccountInfoRequest {

    private String id;
    private String nickname;
    private String imageUrl;

}
