package com.dama.wanderwave.post.request;

import com.dama.wanderwave.place.PlaceRequest;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {

    @Size(max = 100, message = "Title length must be less than or equal to 100 characters")
    @NotEmpty(message = "Title is mandatory")
    private String title;

    @Size(max = 1024, message = "Description length must be less than or equal to 1024 characters")
    private String description;

    private List<String> pros = new ArrayList<>();

    private List<String> cons = new ArrayList<>();

}
