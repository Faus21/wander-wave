package com.dama.wanderwave.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import static com.dama.wanderwave.websocket.WebSocketSettings.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketConfig Tests")
class WebSocketConfigTest {

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Mock
    private AuthChannelInterceptor authChannelInterceptor;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private ChannelRegistration channelRegistration;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private SockJsServiceRegistration sockJsServiceRegistration;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private WebSocketTransportRegistration webSocketTransportRegistration;

//    @BeforeEach
//    void setUp() {
//        webSocketConfig.setMessageBrokerTaskScheduler(taskScheduler);
//    }

    @Nested
    @DisplayName("configureClientInboundChannel Method")
    class ConfigureClientInboundChannelTests {

        @Test
        @DisplayName("Should add authChannelInterceptor to channel registration")
        void configureClientInboundChannelShouldAddInterceptor() {
            webSocketConfig.configureClientInboundChannel(channelRegistration);

            verify(channelRegistration, times(1)).interceptors(authChannelInterceptor);
        }
    }

    @Nested
    @DisplayName("registerStompEndpoints Method")
    class RegisterStompEndpointsTests {

        @Test
        @DisplayName("Should register STOMP endpoint with correct settings")
        void registerStompEndpointsShouldRegisterCorrectly() {
            StompWebSocketEndpointRegistration endpointRegistration = mock(StompWebSocketEndpointRegistration.class);

            // Mock the StompEndpointRegistry for both endpoints
            when(stompEndpointRegistry.addEndpoint(APPLICATION_ENDPOINT.getStringValue()))
                    .thenReturn(endpointRegistration);

            // Mock the StompWebSocketEndpointRegistration
            when(endpointRegistration.setAllowedOrigins("http://localhost:3000", "https://wanderwave.azurewebsites.net/")).thenReturn(endpointRegistration);
            when(endpointRegistration.withSockJS()).thenReturn(sockJsServiceRegistration);

            // Mock the SockJsServiceRegistration
            when(sockJsServiceRegistration.setTaskScheduler(any(TaskScheduler.class))).thenReturn(sockJsServiceRegistration);
            when(sockJsServiceRegistration.setHeartbeatTime(HEARTBEAT_TIME.getIntValue())).thenReturn(sockJsServiceRegistration);
            when(sockJsServiceRegistration.setStreamBytesLimit(STREAM_BYTES_LIMIT.getIntValue())).thenReturn(sockJsServiceRegistration);
            when(sockJsServiceRegistration.setHttpMessageCacheSize(HTTP_MESSAGE_CACHE_SIZE.getIntValue())).thenReturn(sockJsServiceRegistration);
            when(sockJsServiceRegistration.setDisconnectDelay(DISCONNECT_DELAY.getIntValue())).thenReturn(sockJsServiceRegistration);
            when(sockJsServiceRegistration.setSuppressCors(false)).thenReturn(sockJsServiceRegistration);

            // Call the method under test
            webSocketConfig.registerStompEndpoints(stompEndpointRegistry);

            // Verify the method calls
            verify(stompEndpointRegistry).addEndpoint(APPLICATION_ENDPOINT.getStringValue());
            verify(endpointRegistration).setAllowedOrigins("http://localhost:3000", "https://wanderwave.azurewebsites.net/");
            verify(endpointRegistration).withSockJS();
            verify(sockJsServiceRegistration).setTaskScheduler(taskScheduler);
            verify(sockJsServiceRegistration).setHeartbeatTime(HEARTBEAT_TIME.getIntValue());
            verify(sockJsServiceRegistration).setStreamBytesLimit(STREAM_BYTES_LIMIT.getIntValue());
            verify(sockJsServiceRegistration).setHttpMessageCacheSize(HTTP_MESSAGE_CACHE_SIZE.getIntValue());
            verify(sockJsServiceRegistration).setDisconnectDelay(DISCONNECT_DELAY.getIntValue());
            verify(sockJsServiceRegistration).setSuppressCors(false);
        }

    }

    @Nested
    @DisplayName("configureMessageBroker Method")
    class ConfigureMessageBrokerTests {

        @Test
        @DisplayName("Should configure message broker with correct settings")
        void configureMessageBrokerShouldConfigureCorrectly() {
            webSocketConfig.configureMessageBroker(messageBrokerRegistry);

            verify(messageBrokerRegistry, times(1)).setApplicationDestinationPrefixes("/wander_wave");
            verify(messageBrokerRegistry, times(1)).enableSimpleBroker("/topic", "/queue");
        }
    }

    @Nested
    @DisplayName("createWebSocketContainer Method")
    class CreateWebSocketContainerTests {

//        @Test
//        @DisplayName("Should create WebSocket container with correct settings")
//        void createWebSocketContainerShouldCreateCorrectly() {
//            ServletServerContainerFactoryBean container = webSocketConfig.createWebSocketContainer();
//
//            assertThat(container).isNotNull();
//            assertThat(container.getMaxTextMessageBufferSize()).isEqualTo(MAX_BUFFER_SIZE.getIntValue());
//            assertThat(container.getMaxBinaryMessageBufferSize()).isEqualTo(MAX_BUFFER_SIZE.getIntValue());
//        }
    }

    @Nested
    @DisplayName("configureWebSocketTransport Method")
    class ConfigureWebSocketTransportTests {

        @Test
        @DisplayName("Should configure WebSocket transport with correct settings")
        void configureWebSocketTransportShouldConfigureCorrectly() {
            webSocketConfig.configureWebSocketTransport(webSocketTransportRegistration);

            verify(webSocketTransportRegistration, times(1)).setMessageSizeLimit(MESSAGE_SIZE_LIMIT.getIntValue());
            verify(webSocketTransportRegistration, times(1)).setTimeToFirstMessage(TIME_TO_FIRST_MESSAGE.getIntValue());
        }
    }
}