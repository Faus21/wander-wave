package com.dama.wanderwave.post.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommentResponse {

    private AccountInfoResponse accountInfo;
    private String text;
    private LocalDateTime creationDate;

}
