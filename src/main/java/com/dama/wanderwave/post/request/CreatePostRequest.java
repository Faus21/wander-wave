package com.dama.wanderwave.post.request;

import com.dama.wanderwave.place.PlaceRequest;
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
public class CreatePostRequest extends PostRequest{

    @NotEmpty(message = "User is mandatory")
    private String userNickname;

    @NotEmpty(message = "Category type is mandatory")
    private String categoryName;

    private Set<String> hashtags;

    private List<PlaceRequest> places;
}
