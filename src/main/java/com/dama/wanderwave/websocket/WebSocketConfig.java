package com.dama.wanderwave.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import static com.dama.wanderwave.websocket.WebSocketSettings.*;


@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private final AuthChannelInterceptor authChannelInterceptor;

	private  TaskScheduler messageBrokerTaskScheduler;

	@Autowired
	public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler taskScheduler) {
		this.messageBrokerTaskScheduler = taskScheduler;
	}

	@Override
	public void configureClientInboundChannel( ChannelRegistration registration ) {
		registration.interceptors( authChannelInterceptor );
	}

	@Override
	public void registerStompEndpoints( StompEndpointRegistry registry ) {
		registry.addEndpoint(CHAT_ENDPOINT.getStringValue())
				.setAllowedOriginPatterns(ALLOWED_ORIGINS.getStringValue())
				.withSockJS()
				.setSuppressCors(false)
				.setHeartbeatTime(HEARTBEAT_TIME.getIntValue())
				.setTaskScheduler(this.messageBrokerTaskScheduler)
				.setStreamBytesLimit(STREAM_BYTES_LIMIT.getIntValue())
				.setHttpMessageCacheSize(HTTP_MESSAGE_CACHE_SIZE.getIntValue())
				.setDisconnectDelay(DISCONNECT_DELAY.getIntValue());
	}

	@Override
	public void configureMessageBroker( MessageBrokerRegistry config ) {
		config.setApplicationDestinationPrefixes("/wander_wave");
		config.enableSimpleBroker("/topic", "/queue");
	}

	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(MAX_BUFFER_SIZE.getIntValue());
		container.setMaxBinaryMessageBufferSize(MAX_BUFFER_SIZE.getIntValue());
		return container;
	}

	@Override
	public void configureWebSocketTransport( WebSocketTransportRegistration registry ) {
		registry.setMessageSizeLimit(4 * 8192);
		registry.setTimeToFirstMessage(30000);
	}
}
