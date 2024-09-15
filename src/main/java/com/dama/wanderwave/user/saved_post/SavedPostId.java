package com.dama.wanderwave.user.saved_post;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class SavedPostId implements Serializable {

    private String user_id;
    private String post_id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavedPostId that = (SavedPostId) o;
        return Objects.equals(user_id, that.user_id) &&
                       Objects.equals(post_id, that.post_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user_id, post_id);
    }
}
