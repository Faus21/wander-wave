package com.dama.wanderwave.post.response;

import com.dama.wanderwave.comment.Comment;
import com.dama.wanderwave.place.Place;
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

    private List<String> images;

    private LocalDateTime creationDate;

    private String category;

    private String title;

    private String text;

    private AccountInfo accountInfo;

    private String postImage;

    private List<Place> places;

    private Integer likes;

    private Boolean isLiked;

    private Boolean isSaved;

    private List<Comment> comments;

    private String geo;

    private String geoText;

    private String[] pros;

    private String[] cons;

    private Set<String> hashtags;
}