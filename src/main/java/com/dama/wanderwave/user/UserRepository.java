package com.dama.wanderwave.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

	Optional<User> findByEmail( String username);

	boolean existsByNickname( String username );

	boolean existsByEmail( String email );
}
