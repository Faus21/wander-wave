package com.dama.wanderwave.user.saved_post;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class SavedPostId implements Serializable {

    private String user_id;
    private String post_id;
}
