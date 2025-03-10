package com.dama.wanderwave.post.response;

import com.dama.wanderwave.route.Route;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private String id;

    private LocalDateTime creationDate;

    private CategoryResponse category;

    private String title;

    private String text;

    private AccountInfoResponse accountInfo;

    private List<PlaceResponse> places;

    private Integer likes;

    private Integer comments;

    private Boolean isLiked;

    private Boolean isSaved;

    private Boolean isDisableComments;

    private Route route;

    private String[] images;

    private String[] pros;

    private String[] cons;

    private Set<String> hashtags;
}