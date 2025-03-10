package com.dama.wanderwave.post.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCommentRequest {

    @NotBlank(message = "Post id is mandatory")
    private String postId;

    @Size(max = 255, message = "Max length of comment must be not greater than 255")
    @NotBlank(message = "Comment is mandatory")
    private String content;

}
