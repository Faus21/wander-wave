package com.dama.wanderwave.comment;

import com.dama.wanderwave.post.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, String> {

    Page<Comment> findAllByPost(Post post, Pageable pageable);

}
