package com.dama.wanderwave.websocket;

import com.dama.wanderwave.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthChannelInterceptor Tests")
class AuthChannelInterceptorTest {

    @InjectMocks
    private AuthChannelInterceptor authChannelInterceptor;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    private Message<?> message;
    private MessageChannel channel;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        Map<String, List<String>> nativeHeaders = new HashMap<>();
        nativeHeaders.put("Authorization", Collections.singletonList("Bearer validToken"));

        message = MessageBuilder.withPayload("testPayload")
                .setHeader("nativeHeaders", nativeHeaders)
                .build();
        channel = mock(MessageChannel.class);
        userDetails = mock(UserDetails.class);
    }

    @Nested
    @DisplayName("preSend Method")
    class PreSendTests {

        @Test
        @DisplayName("Should authenticate and return message if token is valid")
        void preSendShouldAuthenticateAndReturnMessage() {
            when(jwtService.extractUsername("validToken")).thenReturn("testUser");
            when(userDetailsService.loadUserByUsername("testUser")).thenReturn(userDetails);
            when(jwtService.isTokenValid("validToken", userDetails)).thenReturn(true);

            Message<?> result = authChannelInterceptor.preSend(message, channel);

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(message);
            verify(jwtService, times(1)).extractUsername("validToken");
            verify(userDetailsService, times(1)).loadUserByUsername("testUser");
            verify(jwtService, times(1)).isTokenValid("validToken", userDetails);
        }

        @Test
        @DisplayName("Should return null if token is invalid")
        void preSendShouldReturnNullIfTokenIsInvalid() {
            when(jwtService.extractUsername("validToken")).thenReturn("testUser");
            when(userDetailsService.loadUserByUsername("testUser")).thenReturn(userDetails);
            when(jwtService.isTokenValid("validToken", userDetails)).thenReturn(false);

            Message<?> result = authChannelInterceptor.preSend(message, channel);

            assertThat(result).isNull();
            verify(userDetailsService, times(1)).loadUserByUsername("testUser");
            verify(jwtService, times(1)).isTokenValid("validToken", userDetails);
        }

        @Test
        @DisplayName("Should return null if auth header is missing")
        void preSendShouldReturnNullIfAuthHeaderIsMissing() {
            Message<?> messageWithoutAuthHeader = MessageBuilder.withPayload("testPayload").build();

            Message<?> result = authChannelInterceptor.preSend(messageWithoutAuthHeader, channel);

            assertThat(result).isNull();
            verifyNoInteractions(jwtService, userDetailsService);
        }

        @Test
        @DisplayName("Should return null if auth header does not start with 'Bearer '")
        void preSendShouldReturnNullIfAuthHeaderDoesNotStartWithBearer() {
            Map<String, List<String>> nativeHeaders = new HashMap<>();
            nativeHeaders.put("Authorization", Collections.singletonList("InvalidToken"));

            Message<?> messageWithInvalidHeader = MessageBuilder.withPayload("testPayload")
                    .setHeader("nativeHeaders", nativeHeaders)
                    .build();

            Message<?> result = authChannelInterceptor.preSend(messageWithInvalidHeader, channel);

            assertThat(result).isNull();
            verifyNoInteractions(jwtService, userDetailsService);
        }

        @Test
        @DisplayName("Should return null if user details are null")
        void preSendShouldReturnNullIfUserDetailsAreNull() {
            when(jwtService.extractUsername("validToken")).thenReturn("testUser");
            when(userDetailsService.loadUserByUsername("testUser")).thenReturn(null);

            Message<?> result = authChannelInterceptor.preSend(message, channel);

            assertThat(result).isNull();
            verify(userDetailsService, times(1)).loadUserByUsername("testUser");
            verifyNoMoreInteractions(jwtService);
        }
    }
}