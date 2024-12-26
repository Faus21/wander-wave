package com.dama.wanderwave.post.request;

import com.dama.wanderwave.place.request.PlaceRequest;
import com.dama.wanderwave.place.request.RouteRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRequest {
    private String id;
    private String title;
    private String text;
    @Builder.Default
    private Boolean isDisabledComments = false;
    private Set<String> hashtags;
    private String category;
    private List<MultipartFile> images;
    private List<PlaceRequest> places;
    private RouteRequest route;
    private List<String> pros;
    private List<String> cons;
}