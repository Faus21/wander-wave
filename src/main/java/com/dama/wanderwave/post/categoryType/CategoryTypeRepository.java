package com.dama.wanderwave.post.categoryType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryTypeRepository extends JpaRepository<CategoryType, String> {

    Optional<CategoryType> findByName(String name);
}
