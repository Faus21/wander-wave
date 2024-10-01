package com.dama.wanderwave.refresh_token;

import com.dama.wanderwave.handler.TokenRefreshException;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setNickname("testUser");

        refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken("testToken");
        refreshToken.setExpiresAt(Instant.now().plusMillis(1000000));

        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenDurationMs", 1000000L);
    }

    @Test
    void findByToken_ShouldReturnRefreshToken_WhenTokenExists() {
        when(refreshTokenRepository.findByToken("testToken")).thenReturn(Optional.of(refreshToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken("testToken");

        assertTrue(result.isPresent());
        assertEquals(refreshToken, result.get());
        verify(refreshTokenRepository, times(1)).findByToken("testToken");
    }

    @Test
    void findByToken_ShouldReturnEmpty_WhenTokenDoesNotExist() {
        when(refreshTokenRepository.findByToken("nonExistentToken")).thenReturn(Optional.empty());

        Optional<RefreshToken> result = refreshTokenService.findByToken("nonExistentToken");

        assertFalse(result.isPresent());
        verify(refreshTokenRepository, times(1)).findByToken("nonExistentToken");
    }

    @Test
    void createRefreshToken_ShouldCreateAndReturnRefreshToken() {
        Map<String, Object> claims = Map.of("username", user.getNickname());
        String newRefreshToken = "newRefreshToken";
        when(jwtService.generateRefreshToken(claims, user)).thenReturn(newRefreshToken);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(user);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertNotNull(result.getExpiresAt());
        assertTrue(result.getExpiresAt().isAfter(Instant.now()));
        assertEquals(newRefreshToken, result.getToken());
        verify(jwtService, times(1)).generateRefreshToken(claims, user);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void verifyExpiration_ShouldReturnToken_WhenTokenIsValid() {
        RefreshToken result = refreshTokenService.verifyExpiration(refreshToken);

        assertNotNull(result);
        assertEquals(refreshToken, result);
        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }

    @Test
    void verifyExpiration_ShouldThrowException_WhenTokenIsExpired() {
        refreshToken.setExpiresAt(Instant.now().minusMillis(1000000));

        TokenRefreshException exception = assertThrows(TokenRefreshException.class, () -> refreshTokenService.verifyExpiration(refreshToken));

        assertEquals("Refresh token was expired. Please make a new sign-in request", exception.getMessage());
        verify(refreshTokenRepository, times(1)).delete(refreshToken);
    }

    @Test
    void deleteExpiredTokens_ShouldDeleteExpiredTokens() {
        when(refreshTokenRepository.deleteByExpiresAtBefore(any(Instant.class))).thenReturn(5);

        int result = refreshTokenService.deleteExpiredTokens();

        assertEquals(5, result);
        verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any(Instant.class));
    }

    @Test
    void deleteExpiredTokens_ShouldReturnZero_WhenNoTokensAreExpired() {
        when(refreshTokenRepository.deleteByExpiresAtBefore(any(Instant.class))).thenReturn(0);

        int result = refreshTokenService.deleteExpiredTokens();

        assertEquals(0, result);
        verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any(Instant.class));
    }

    @Test
    void createRefreshToken_ShouldHandleNullUser() {
        assertThrows(NullPointerException.class, () -> refreshTokenService.createRefreshToken(null));

        verify(jwtService, never()).generateRefreshToken(anyMap(), any(User.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void verifyExpiration_ShouldHandleNullToken() {
        assertThrows(NullPointerException.class, () -> refreshTokenService.verifyExpiration(null));

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }
}