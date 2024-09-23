package com.dama.wanderwave.refresh_token;


import com.dama.wanderwave.handler.TokenRefreshException;
import com.dama.wanderwave.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.dama.wanderwave.user.User;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RefreshTokenService {

    private final JwtService jwtService;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private Long refreshTokenDurationMs;


    private final RefreshTokenRepository refreshTokenRepository;


    public Optional<RefreshToken> findByToken( String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenDurationMs));

        Map<String, Object> claims = Map.of("username", user.getNickname());
        String token = jwtService.generateRefreshToken(claims, user);
        refreshToken.setToken(token);

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token was expired. Please make a new sign-in" +
                                                    " request");
        }

        return token;
    }

    @Scheduled(cron = "0 0 0 * * 1-7")
    @Transactional
    public int deleteExpiredTokens() {
        return refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
