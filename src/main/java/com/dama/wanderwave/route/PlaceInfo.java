package com.dama.wanderwave.route;

import com.dama.wanderwave.place.request.PlaceInfoRequest;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Embeddable
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceInfo {

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "location_name")
    private String locationName;

    @Embedded
    private Coords coords;

    public static PlaceInfo fromPlaceInfoRequest(PlaceInfoRequest placeInfoRequest) {
        return PlaceInfo.builder()
                .displayName(placeInfoRequest.getDisplayName())
                .locationName(placeInfoRequest.getLocationName())
                .coords(Coords.fromCoordsRequest(placeInfoRequest.getCoords()))
                .build();
    }
}