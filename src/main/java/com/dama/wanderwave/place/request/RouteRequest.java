package com.dama.wanderwave.place.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteRequest {
    private PlaceInfoRequest sourceCoords;
    private PlaceInfoRequest destinationCoords;
    private String description;
}