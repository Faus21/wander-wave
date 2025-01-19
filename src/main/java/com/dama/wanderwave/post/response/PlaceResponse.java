package com.dama.wanderwave.post.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceResponse {

    private String displayName;

    private String description;

    private String locationName;

    private Double rating;

    private CoordsResponse coords;

}
