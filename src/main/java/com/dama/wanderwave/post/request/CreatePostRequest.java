package com.dama.wanderwave.post.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePostRequest {

    @Size(max = 100, message = "Title length must be less than or equal to 100 characters")
    @NotEmpty(message = "Title is mandatory")
    private String title;

    @NotEmpty(message = "User is mandatory")
    private String userNickname;

    @Size(max = 2048, message = "Description length must be less than or equal to 1024 characters")
    private String description;

    @NotEmpty(message = "Category type is mandatory")
    private String categoryName;

    private List<String> pros;

    private List<String> cons;

    private Set<String> hashtags;
}
