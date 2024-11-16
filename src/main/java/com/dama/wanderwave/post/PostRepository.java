package com.dama.wanderwave.post;

import com.dama.wanderwave.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.id = :postId")
    Optional<Post> findByIdWithLikes(@Param("postId") String postId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.savedPosts WHERE p.id = :postId")
    Optional<Post> findByIdSaved(@Param("postId") String postId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.hashtags WHERE p.user = :user")
    List<Post> findByUserWithHashtags(@Param("user") User user);

    @Query("SELECT p FROM Post p INNER JOIN FETCH p.likes l WHERE l.user = :user")
    List<Post> findByUserWithLikes(@Param("user") User user);

    @Query("SELECT p FROM Post p INNER JOIN FETCH p.savedPosts s WHERE s.user = :user")
    List<Post> findByUserSaved(@Param("user") User user);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.hashtags h WHERE h.id=:hashtag")
    Page<Post> findByHashtag(@Param("user") String hashtag, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.categoryType c WHERE c.name=:category")
    Page<Post> findByCategory(@Param("user") String category, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN p.likes l GROUP BY p ORDER BY COUNT(l) DESC, p.createdAt DESC")
    Page<Post> findMostPopularPostsByLikes(Pageable pageable);
}
