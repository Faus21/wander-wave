package com.dama.wanderwave.post.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {

    private AccountInfoResponse accountInfo;
    private String text;
    private LocalDateTime creationDate;

}
