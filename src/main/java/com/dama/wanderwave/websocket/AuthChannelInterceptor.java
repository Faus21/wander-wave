package com.dama.wanderwave.websocket;

import com.dama.wanderwave.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthChannelInterceptor implements ChannelInterceptor {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		String authHeader = (String) message.getHeaders().get("simpSessionAttributes.Authorization");

		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring("Bearer ".length());
			String username = jwtService.extractUsername(token);

			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			if (userDetails != null && jwtService.isTokenValid(token, userDetails)) {
				log.info("WebSocket message authenticated for user: {}", username);
				return message;
			}
		}

		log.warn("WebSocket message rejected due to invalid authentication");
		return null;
	}
}
