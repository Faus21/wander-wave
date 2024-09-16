package com.dama.wanderwave.auth;

import com.dama.wanderwave.email.EmailService;
import com.dama.wanderwave.role.RoleRepository;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.token.TokenRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TokenRepository tokenRepository;
    @Mock
    private EmailService emailService;

    @Test
    void findByEmailShouldReturnNullWithNonExistentEmail() throws Exception {
        String mockEmail = "test@test.com";
        when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.empty());

        User user = userRepository.findByEmail(mockEmail).orElse(null);

        verify(userRepository, times(1)).findByEmail(mockEmail);
        assertNull(user);
    }

    @Test
    void findByEmailShouldReturnUserWithExistentEmail() throws Exception {
        String mockEmail = "test@test.com";
        User mockUser = new User();
        when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockUser));

        User user = userRepository.findByEmail(mockEmail).orElse(null);

        verify(userRepository, times(1)).findByEmail(mockEmail);
        assertSame(user, mockUser);
    }

}
