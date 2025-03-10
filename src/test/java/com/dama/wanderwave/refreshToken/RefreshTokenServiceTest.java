package com.dama.wanderwave.refreshToken;

import com.dama.wanderwave.handler.token.TokenRefreshException;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
@DisplayName("RefreshTokenService Tests")
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

    @Nested
    @DisplayName("findByToken Method")
    class FindByTokenTests {

        @Test
        @DisplayName("Should Return RefreshToken When Token Exists")
        void findByToken_ShouldReturnRefreshToken_WhenTokenExists() {
            when(refreshTokenRepository.findByToken("testToken")).thenReturn(Optional.of(refreshToken));

            Optional<RefreshToken> result = refreshTokenService.findByToken("testToken");

            assertTrue(result.isPresent());
            assertEquals(refreshToken, result.get());
            verify(refreshTokenRepository, times(1)).findByToken("testToken");
        }

        @Test
        @DisplayName("Should Return Empty When Token Does Not Exist")
        void findByToken_ShouldReturnEmpty_WhenTokenDoesNotExist() {
            when(refreshTokenRepository.findByToken("nonExistentToken")).thenReturn(Optional.empty());

            Optional<RefreshToken> result = refreshTokenService.findByToken("nonExistentToken");

            assertFalse(result.isPresent());
            verify(refreshTokenRepository, times(1)).findByToken("nonExistentToken");
        }
    }

    @Nested
    @DisplayName("createRefreshToken Method")
    class CreateRefreshTokenTests {

        @Test
        @DisplayName("Should Create And Return RefreshToken")
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
        @DisplayName("Should Handle Null User")
        void createRefreshToken_ShouldHandleNullUser() {
            assertThrows(NullPointerException.class, () -> refreshTokenService.createRefreshToken(null));

            verify(jwtService, never()).generateRefreshToken(anyMap(), any(User.class));
            verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("verifyExpiration Method")
    class VerifyExpirationTests {

        @Test
        @DisplayName("Should Return Token When Token Is Valid")
        void verifyExpiration_ShouldReturnToken_WhenTokenIsValid() {
            RefreshToken result = refreshTokenService.verifyExpiration(refreshToken);

            assertNotNull(result);
            assertEquals(refreshToken, result);
            verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        }

        @Test
        @DisplayName("Should Throw Exception When Token Is Expired")
        void verifyExpiration_ShouldThrowException_WhenTokenIsExpired() {
            refreshToken.setExpiresAt(Instant.now().minusMillis(1000000));

            TokenRefreshException exception = assertThrows(TokenRefreshException.class, () -> refreshTokenService.verifyExpiration(refreshToken));

            assertEquals("Refresh token was expired. Please make a new sign-in request", exception.getMessage());
            verify(refreshTokenRepository, times(1)).delete(refreshToken);
        }

        @Test
        @DisplayName("Should Handle Null Token")
        void verifyExpiration_ShouldHandleNullToken() {
            assertThrows(NullPointerException.class, () -> refreshTokenService.verifyExpiration(null));

            verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
        }
    }

    @Nested
    @DisplayName("deleteExpiredTokens Method")
    class DeleteExpiredTokensTests {

        @Test
        @DisplayName("Should Delete Expired Tokens")
        void deleteExpiredTokens_ShouldDeleteExpiredTokens() {
            when(refreshTokenRepository.deleteByExpiresAtBefore(any(Instant.class))).thenReturn(5);

            int result = refreshTokenService.deleteExpiredTokens();

            assertEquals(5, result);
            verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any(Instant.class));
        }

        @Test
        @DisplayName("Should Return Zero When No Tokens Are Expired")
        void deleteExpiredTokens_ShouldReturnZero_WhenNoTokensAreExpired() {
            when(refreshTokenRepository.deleteByExpiresAtBefore(any(Instant.class))).thenReturn(0);

            int result = refreshTokenService.deleteExpiredTokens();

            assertEquals(0, result);
            verify(refreshTokenRepository, times(1)).deleteByExpiresAtBefore(any(Instant.class));
        }
    }
}