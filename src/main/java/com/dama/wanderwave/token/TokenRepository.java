package com.dama.wanderwave.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<EmailToken, String> {
	Optional<EmailToken> findByContent( String content);
}
