package com.dama.wanderwave.user.like;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class LikeId implements Serializable {

    private String user_id;
    private String post_id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LikeId likeId = (LikeId) o;
        return Objects.equals(user_id, likeId.user_id) &&
                       Objects.equals(post_id, likeId.post_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user_id, post_id);
    }
}
