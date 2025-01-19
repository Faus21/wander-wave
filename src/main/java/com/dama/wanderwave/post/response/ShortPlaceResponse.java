package com.dama.wanderwave.post.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShortPlaceResponse {

    private String displayName;

    private Double rating;

}
