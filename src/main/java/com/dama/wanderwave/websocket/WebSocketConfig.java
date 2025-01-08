package com.dama.wanderwave.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
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

	private TaskScheduler taskScheduler;

	@Autowired
	public void setMessageBrokerTaskScheduler(@Lazy TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(authChannelInterceptor);
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint(APPLICATION_ENDPOINT.getStringValue())
				.setAllowedOrigins("http://localhost:3000", "https://wanderwave.azurewebsites.net/")
				.withSockJS()
				.setTaskScheduler(taskScheduler)
				.setHeartbeatTime(HEARTBEAT_TIME.getIntValue())
				.setStreamBytesLimit(STREAM_BYTES_LIMIT.getIntValue())
				.setHttpMessageCacheSize(HTTP_MESSAGE_CACHE_SIZE.getIntValue())
				.setDisconnectDelay(DISCONNECT_DELAY.getIntValue())
				.setSuppressCors(false);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.setApplicationDestinationPrefixes("/wander_wave");
		config.enableSimpleBroker("/topic", "/queue");
		config.setUserDestinationPrefix("/user");
	}

	@Bean
	public ServletServerContainerFactoryBean createWebSocketContainer() {
		ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
		container.setMaxTextMessageBufferSize(MAX_BUFFER_SIZE.getIntValue());
		container.setMaxBinaryMessageBufferSize(MAX_BUFFER_SIZE.getIntValue());
		return container;
	}

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
		registry.setMessageSizeLimit(MESSAGE_SIZE_LIMIT.getIntValue());
		registry.setTimeToFirstMessage(TIME_TO_FIRST_MESSAGE.getIntValue());
	}
}