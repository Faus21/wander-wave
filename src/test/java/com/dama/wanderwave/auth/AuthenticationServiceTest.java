package com.dama.wanderwave.auth;


import com.dama.wanderwave.email.EmailService;
import com.dama.wanderwave.handler.TokenExpiredException;
import com.dama.wanderwave.token.Token;
import com.dama.wanderwave.token.TokenRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;


    @Test
    void findUserByEmailShouldReturnNullWithNonExistentEmail() {
        String mockEmail = "test@test.com";
        when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.empty());

        User user = userRepository.findByEmail(mockEmail).orElse(null);

        verify(userRepository, times(1)).findByEmail(mockEmail);
        assertNull(user);
    }

    @Test
    void findUserByEmailShouldReturnUserWithExistentEmail() {
        String mockEmail = "test@test.com";
        User mockUser = new User();
        when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockUser));

        User user = userRepository.findByEmail(mockEmail).orElse(null);

        verify(userRepository, times(1)).findByEmail(mockEmail);
        assertSame(user, mockUser);
    }
    
    @Test
    void findExistingTokenShouldBeOk() {
        String mockContent = "testToken";
        Token mockToken = new Token();
        when(tokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockToken));

        Token token = tokenRepository.findByContent(mockContent).orElse(null);

        verify(tokenRepository, times(1)).findByContent(mockContent);
        assertSame(mockToken, token);
    }

    @Test
    void findNonExistingTokenShouldBeNull() {
        String mockContent = "testToken";
        when(tokenRepository.findByContent(mockContent)).thenReturn(Optional.empty());

        Token token = tokenRepository.findByContent(mockContent).orElse(null);

        verify(tokenRepository, times(1)).findByContent(mockContent);
        assertNull(token);
    }

    @Test
    void findUserByIdShouldReturnNullWithNonExistentId() {
        String mockId = "mockId";
        when(userRepository.findById(mockId)).thenReturn(Optional.empty());

        User user = userRepository.findById(mockId).orElse(null);

        verify(userRepository, times(1)).findById(mockId);
        assertNull(user);
    }

    @Test
    void findUserByIdShouldReturnUserWithExistentId() {
        String mockId = "mockId";
        User mockUser = new User();
        when(userRepository.findById(mockId)).thenReturn(Optional.of(mockUser));

        User user = userRepository.findById(mockId).orElse(null);

        verify(userRepository, times(1)).findById(mockId);
        assertSame(user, mockUser);
    }

    @Test
    void saveTokenShouldBeOk() {
        Token mockToken = Token.builder()
                .content("mockToken")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(new User())
                .build();

        when(tokenRepository.save(mockToken)).thenReturn(mockToken);

        Token token = tokenRepository.save(mockToken);

        verify(tokenRepository, times(1)).save(mockToken);
        assertNotNull(token);
        assertSame(mockToken, token);
    }

    @Test
    void changePasswordWithWrongTokenShouldFail() {
        String mockToken = "mockToken";
        String mockPassword = "mockPassword";

        assertThrows(RuntimeException.class, () -> authenticationService.changeUserPassword(mockToken, mockPassword));
    }

    @Test
    void changePasswordWithExpiredTokenShouldFail() {
        String mockContent = "mockContent";
        Token mockToken = Token.builder()
                .content(mockContent)
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .user(new User())
                .build();
        String mockPassword = "mockPassword";

        when(tokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockToken));

        assertThrows(TokenExpiredException.class, () -> authenticationService.changeUserPassword(mockContent, mockPassword));
    }

    @Test
    void changePasswordWithCorrectTokenShouldBeOk() {
        String mockContent = "mockContent";
        User mockUser = new User();
        mockUser.setId("mockId");
        Token mockToken = Token.builder()
                .content(mockContent)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(mockUser)
                .build();
        String mockPassword = "mockPassword";

        when(userRepository.findById(mockUser.getId())).thenReturn(Optional.of(mockUser));
        when(tokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockToken));

        assertDoesNotThrow(() -> authenticationService.changeUserPassword(mockContent, mockPassword));
    }

    @Test
    void recoverUserWithNonExistentEmailShouldBeOk() {
        String mockEmail = "test@test.com";
        when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.empty());

        User user = userRepository.findByEmail(mockEmail).orElse(null);

        verify(userRepository, times(1)).findByEmail(mockEmail);
        assertNull(user);
        assertDoesNotThrow(() -> authenticationService.recoverAccount(mockEmail));
    }

    @Test
    void recoverUserWithExistentEmailShouldBeOk() {
        String mockEmail = "test@test.com";
        User mockUser = new User();
        when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockUser));

        User user = userRepository.findByEmail(mockEmail).orElse(null);

        verify(userRepository, times(1)).findByEmail(mockEmail);
        assertSame(mockUser, user);
        assertDoesNotThrow(() -> authenticationService.recoverAccount(mockEmail));
    }

}
