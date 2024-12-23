package com.dama.wanderwave.post.response.dto;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryResponse {

    private String name;
    private String imageUrl;

}
