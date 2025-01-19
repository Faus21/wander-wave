package com.dama.wanderwave.websocket;

import com.dama.wanderwave.security.JwtService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class AuthChannelInterceptor implements ChannelInterceptor {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	public Message<?> preSend(Message<?> message, @NonNull MessageChannel channel) {
		log.debug("Intercepting WebSocket message: {}", message);

		Map<String, Object> headers = message.getHeaders();
		log.debug("All headers: {}", headers);


		Map<String, List<String>> nativeHeaders = (Map<String, List<String>>) headers.get("nativeHeaders");

		if (nativeHeaders != null) {
			List<String> authHeaders = nativeHeaders.get("Authorization");
			if (authHeaders != null && !authHeaders.isEmpty()) {
				String authHeader = authHeaders.getFirst();
				log.debug("Authorization header: {}", authHeader);

				if (authHeader.startsWith("Bearer ")) {
					String token = authHeader.substring("Bearer ".length());
					log.debug("Extracted token: {}", token);

					String username = jwtService.extractUsername(token);
					log.debug("Extracted username from token: {}", username);

					if (username != null) {
						UserDetails userDetails = userDetailsService.loadUserByUsername(username);
						log.debug("Loaded user details for username: {}", username);

						if (userDetails != null) {
							log.debug("Validating token for user: {}", username);
							if (jwtService.isTokenValid(token, userDetails)) {
								log.info("WebSocket message authenticated for user: {}", username);
								return message;
							} else {
								log.warn("Token is invalid for user: {}", username);
							}
						} else {
							log.warn("User details not found for username: {}", username);
						}
					} else {
						log.warn("Username not found in token");
					}
				} else {
					log.warn("Authorization header does not start with 'Bearer '");
				}
			} else {
				log.warn("Authorization header is missing in nativeHeaders");
			}
		} else {
			log.warn("Native headers are missing in the message");
		}

		log.warn("WebSocket message rejected due to authentication failure");
		return null;
	}
}