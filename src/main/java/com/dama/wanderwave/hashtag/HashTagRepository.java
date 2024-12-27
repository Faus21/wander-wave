package com.dama.wanderwave.hashtag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface HashTagRepository extends JpaRepository<HashTag, String> {
    Optional<HashTag> findByTitle(String hashTag);

    @Query("SELECT h FROM HashTag h WHERE h.title LIKE :prefix%")
    Page<HashTag> findByTitleStartingWith(@Param("prefix") String prefix, Pageable pageable);
}
