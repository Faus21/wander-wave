package com.dama.wanderwave.post;

import com.dama.wanderwave.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, String> {

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes WHERE p.id = :postId")
    Optional<Post> findByIdWithLikes(@Param("postId") String postId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.savedPosts WHERE p.id = :postId")
    Optional<Post> findByIdSaved(@Param("postId") String postId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.hashtags " +
            "WHERE p.user = :user")
    Page<Post> findByUserWithHashtags(@Param("user") User user, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.likes l " +
            "LEFT JOIN FETCH p.hashtags h WHERE l.user = :user")
    Page<Post> findByUserWithLikes(@Param("user") User user, Pageable page);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.savedPosts s " +
            "LEFT JOIN FETCH p.hashtags h WHERE s.user = :user")
    Page<Post> findByUserSaved(@Param("user") User user, Pageable pageable);


    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.hashtags h WHERE h.id=:hashtag")
    Page<Post> findByHashtag(@Param("user") String hashtag, Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.categoryType c " +
            "LEFT JOIN FETCH p.hashtags h " +
            "WHERE c.name = :category")
    Page<Post> findByCategory(@Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Post p LEFT JOIN p.likes l LEFT JOIN FETCH p.hashtags h " +
            "WHERE p.createdAt >= :lastWeek")
    Page<Post> findPopularPosts(Pageable pageable, @Param("lastWeek") LocalDateTime lastWeek);

}
