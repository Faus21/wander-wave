package com.dama.wanderwave.post.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortPostResponse {

    private String id;

    private LocalDateTime creationDate;

    private CategoryResponse category;

    private String title;

    private PlaceResponse place;

    private Double rating;

    private Integer commentsCount;

    private AccountInfoResponse accountInfo;

    private Integer likes;

    private Boolean isLiked;

    private Boolean isSaved;


}
