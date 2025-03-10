package com.dama.wanderwave.refreshToken;

import com.dama.wanderwave.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken( String token);

    @Transactional
    void deleteByUser( User user);

    @Modifying
    @Transactional
    int deleteByExpiresAtBefore( Instant  now);

}
