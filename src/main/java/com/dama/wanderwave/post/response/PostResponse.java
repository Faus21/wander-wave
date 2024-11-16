package com.dama.wanderwave.post.response;

import com.dama.wanderwave.hashtag.HashTag;
import com.dama.wanderwave.post.categoryType.CategoryType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class PostResponse {

    private String id;

    private String title;

    private LocalDateTime createdAt;

    private String nickname;

    private String description;

    private String categoryType;

    private String[] pros;

    private String[] cons;

    private Set<String> hashtags;
}
