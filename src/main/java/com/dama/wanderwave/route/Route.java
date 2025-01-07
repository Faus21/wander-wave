package com.dama.wanderwave.route;

import com.dama.wanderwave.place.request.RouteRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routes")
public class Route {

    @Id
    @GeneratedValue(generator = "hash_generator")
    @GenericGenerator(name = "hash_generator", type = com.dama.wanderwave.hash.HashUUIDGenerator.class)
    @Column(name = "route_id", nullable = false, updatable = false, unique = true)
    private String routeId;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "displayName", column = @Column(name = "source_coords_display_name")),
            @AttributeOverride(name = "locationName", column = @Column(name = "source_coords_location_name")),
            @AttributeOverride(name = "coords.latitude", column = @Column(name = "source_coords_latitude")),
            @AttributeOverride(name = "coords.longitude", column = @Column(name = "source_coords_longitude"))
    })
    private PlaceInfo sourceCoords;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "displayName", column = @Column(name = "destination_coords_display_name")),
            @AttributeOverride(name = "locationName", column = @Column(name = "destination_coords_location_name")),
            @AttributeOverride(name = "coords.latitude", column = @Column(name = "destination_coords_latitude")),
            @AttributeOverride(name = "coords.longitude", column = @Column(name = "destination_coords_longitude"))
    })
    private PlaceInfo destinationCoords;

    @Column(name = "description", length = 500)
    private String description;

    public static Route fromRouteRequest(RouteRequest routeRequest) {
        return Route.builder()
                .sourceCoords(PlaceInfo.fromPlaceInfoRequest(routeRequest.getSourceCoords()))
                .destinationCoords(PlaceInfo.fromPlaceInfoRequest(routeRequest.getDestinationCoords()))
                .description(routeRequest.getDescription())
                .build();
    }
}