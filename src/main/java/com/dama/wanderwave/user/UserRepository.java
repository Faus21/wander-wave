package com.dama.wanderwave.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

	Optional<User> findByNickname(String nickname);

    boolean existsByNickname(String username);

    boolean existsByEmail(String email);

    Optional<User> findByNicknameOrEmail(String username, String email);

    @Query("select u from User u where u.nickname = :userData or u.email = :userData")
    Optional<User> loadByNicknameOrEmail(@Param("userData") String userData);
}
