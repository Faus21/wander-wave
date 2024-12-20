package com.dama.wanderwave.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
		if (isAuthenticationPath(request)) {
			filterChain.doFilter(request, response);
			return;
		}

		String jwt = getJwtFromRequest(request);
		if (jwt == null) {
			filterChain.doFilter(request, response);
			return;
		}

		String userEmail = jwtService.extractUsername(jwt);
		if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			authenticateUser(userEmail, jwt, request);
		}

		filterChain.doFilter(request, response);
	}

	private boolean isAuthenticationPath(HttpServletRequest request) {
		return request.getServletPath().contains("/api/auth") &&
				!request.getRequestURI().contains("logout");
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		final String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return authHeader.substring(7);
		}
		return null;
	}

	private void authenticateUser(String userEmail, String jwt, HttpServletRequest request) {
		UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
		if (jwtService.isTokenValid(jwt, userDetails)) {
			UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
					userDetails,
					null,
					userDetails.getAuthorities()
			);
			authToken.setDetails(
					new WebAuthenticationDetailsSource().buildDetails(request)
			);
			SecurityContextHolder.getContext().setAuthentication(authToken);
		}
	}
}
