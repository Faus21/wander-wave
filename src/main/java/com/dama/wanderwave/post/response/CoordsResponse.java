package com.dama.wanderwave.post.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CoordsResponse {

    private BigDecimal latitude;
    private BigDecimal longitude;

}
