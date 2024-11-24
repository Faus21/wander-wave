package com.dama.wanderwave.place;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlaceRequest {

    @Size(max = 500, message = "Place description length must be less than or equal to 500 characters")
    @NotBlank(message = "Place description cannot be blank")
    private String description;

    @Min(1)
    @Max(5)
    private double rating;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String imageUrl;
}
