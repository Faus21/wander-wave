package com.dama.wanderwave.post.response;

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
