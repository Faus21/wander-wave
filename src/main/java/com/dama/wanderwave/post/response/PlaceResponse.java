package com.dama.wanderwave.post.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceResponse {

    private String description;

    private double rating;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String imgUrl;

}
