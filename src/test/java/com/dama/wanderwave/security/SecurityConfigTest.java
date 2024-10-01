package com.dama.wanderwave.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private AuthenticationProvider authenticationProvider;

    @Mock
    private JwtFilter jwtFilter;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Mock
    private HttpSecurity httpSecurity;

    @Nested
    @DisplayName("When building the SecurityFilterChain")
    class SecurityFilterChainTests {

        @Test
        @DisplayName("should configure HttpSecurity correctly")
        void securityFilterChain() throws Exception {
            when(httpSecurity.cors(any())).thenReturn(httpSecurity);
            when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
            when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
            when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
            when(httpSecurity.authenticationProvider(any(AuthenticationProvider.class))).thenReturn(httpSecurity);
            when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
            when(httpSecurity.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

            SecurityFilterChain filterChain = securityConfig.securityFilterChain(httpSecurity);

            assertThat(filterChain).isNotNull();
            verify(httpSecurity).cors(any());
            verify(httpSecurity).csrf(any());
            verify(httpSecurity).authorizeHttpRequests(any());
            verify(httpSecurity).sessionManagement(any());
            verify(httpSecurity).authenticationProvider(authenticationProvider);
            verify(httpSecurity).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
            verify(httpSecurity).build();
        }
    }
}
