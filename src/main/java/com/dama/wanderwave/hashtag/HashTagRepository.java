package com.dama.wanderwave.hashtag;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HashTagRepository extends JpaRepository<HashTag, String> {
    Optional<HashTag> findByTitle(String hashTag);
}
