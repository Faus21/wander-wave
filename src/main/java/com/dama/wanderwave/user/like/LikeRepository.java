package com.dama.wanderwave.user.like;

import com.dama.wanderwave.post.Post;
import com.dama.wanderwave.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, LikeId> {
    Optional<Like> findByUserAndPost(User user, Post post);

}
