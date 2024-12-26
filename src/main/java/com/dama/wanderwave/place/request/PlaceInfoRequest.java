package com.dama.wanderwave.place.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceInfoRequest {
    private String displayName;
    private String locationName;
    private CoordsRequest coords;
}