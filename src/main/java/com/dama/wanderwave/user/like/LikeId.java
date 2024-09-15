package com.dama.wanderwave.user.like;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class LikeId implements Serializable {

    private String user_id;
    private String post_id;

}
