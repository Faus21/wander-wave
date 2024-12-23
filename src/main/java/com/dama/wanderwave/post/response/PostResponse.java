package com.dama.wanderwave.post.response;

import com.dama.wanderwave.comment.Comment;
import com.dama.wanderwave.post.response.dto.AccountInfoResponse;
import com.dama.wanderwave.post.response.dto.CategoryResponse;
import com.dama.wanderwave.post.response.dto.PlaceResponse;
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

    private Boolean isLiked;

    private Boolean isSaved;

    private List<Comment> comments;

    private String geo;

    private String geoText;

    private String[] pros;

    private String[] cons;

    private Set<String> hashtags;
}