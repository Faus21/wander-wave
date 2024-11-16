package com.dama.wanderwave.user.saved_post;

import com.dama.wanderwave.post.Post;
import com.dama.wanderwave.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SavedPostRepository extends JpaRepository<SavedPost, SavedPostId> {
    Optional<SavedPost> findByUserAndPost(User user, Post post);
}
