package com.dama.wanderwave.post.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShortPostResponse {

    private String id;

    private LocalDateTime creationDate;

    private CategoryResponse category;

    private String title;

    private ShortPlaceResponse place;

    private Double rating;

    private Integer commentsCount;

    private String previewImage;

    private AccountInfoResponse accountInfo;

    private Integer likes;

    private Boolean isLiked;

    private Boolean isSaved;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ShortPostResponse that = (ShortPostResponse) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
