package com.dama.wanderwave.route;

import com.dama.wanderwave.place.request.CoordsRequest;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coords {

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    public static Coords fromCoordsRequest(CoordsRequest coordsRequest) {
        return Coords.builder()
                .latitude(BigDecimal.valueOf(coordsRequest.getLatitude()))
                .longitude(BigDecimal.valueOf(coordsRequest.getLongitude()))
                .build();
    }
}