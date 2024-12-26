package com.dama.wanderwave.place.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoordsRequest {
    private double latitude;
    private double longitude;
}