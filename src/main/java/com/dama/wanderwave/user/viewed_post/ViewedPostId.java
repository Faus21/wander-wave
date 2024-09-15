package com.dama.wanderwave.user.viewed_post;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class ViewedPostId implements Serializable {

    private String user_id;
    private String post_id;

}
