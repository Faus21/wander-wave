package com.dama.wanderwave.auth;

import com.dama.wanderwave.handler.TokenExpiredException;
import com.dama.wanderwave.role.RoleRepository;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.token.Token;
import com.dama.wanderwave.token.TokenRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthenticationService {

	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final RoleRepository roleRepository;
	private final TokenRepository tokenRepository;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public void register( @Valid RegistrationRequest request ) {
		log.info("Registering new user: {}", request.getEmail());

		User user = createUser(request);
		userRepository.save(user);

		log.info("User registered successfully: {}", user.getEmail());

		try {
			sendValidationEmail(user);
		} catch ( MessagingException e ) {
			log.error("Failed to send validation email to user: {}", user.getEmail(), e);
		}
	}

	private User createUser( RegistrationRequest request ) {
		log.debug("Creating user with email: {}", request.getEmail());

		var userRole = roleRepository.findByName("USER").orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));

		return User.builder().nickname(request.getUsername()).email(request.getEmail()).password(passwordEncoder.encode(request.getPassword())).accountLocked(false).enabled(false).roles(Set.of(userRole)).build();
	}

	public AuthenticationResponse authenticate( AuthenticationRequest request ) {
		log.info("Authenticating user: {}", request.getEmail());

		var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		var user = (User) auth.getPrincipal();
		var claims = createClaims(user);

		var jwtToken = jwtService.generateToken(claims, user);
		log.info("User authenticated successfully: {}", user.getEmail());

		return AuthenticationResponse.builder().token(jwtToken).build();
	}

	private Map<String, Object> createClaims( User user ) {
		var claims = new HashMap<String, Object>();
		claims.put("username", user.getNickname());
		return claims;
	}

	@Transactional
	public void activateAccount( String token ) throws MessagingException {
		log.info("Activating account with token: {}", token);

		Token savedToken = findTokenOrThrow(token);

		if ( LocalDateTime.now().isAfter(savedToken.getExpiresAt()) ) {
			log.warn("Token expired for user: {}", savedToken.getUser().getEmail());
			sendValidationEmail(savedToken.getUser());
			throw new TokenExpiredException("Activation token has expired. A new token has been sent to the same " + "email address.");
		}

		activateUser(savedToken.getUser());
		markTokenAsValidated(savedToken);

		log.info("Account activated successfully for user: {}", savedToken.getUser().getEmail());
	}

	private Token findTokenOrThrow( String token ) {
		return tokenRepository.findByContent(token).orElseThrow(() -> new RuntimeException("Invalid token"));
	}

	private void activateUser( User user ) {
		log.debug("Activating user with email: {}", user.getEmail());

		var userToUpdate = userRepository.findById(user.getId()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
		userToUpdate.setEnabled(true);
		userRepository.save(userToUpdate);
	}

	private void markTokenAsValidated( Token token ) {
		token.setValidatedAt(LocalDateTime.now());
		tokenRepository.save(token);

		log.debug("Token marked as validated for user: {}", token.getUser().getEmail());
	}

	private String generateAndSaveActivationToken( User user ) {
		String tokenContent = generateActivationCode(6);
		Token token = Token.builder().content(tokenContent).createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(15)).user(user).build();
		tokenRepository.save(token);

		log.debug("Generated and saved activation token for user: {}", user.getEmail());

		return tokenContent;
	}

	private void sendValidationEmail( User user ) throws MessagingException {
		String newToken = generateAndSaveActivationToken(user);
		// TODO: Implement email sending logic using newToken and activationUrl

		log.info("Sent validation email to user: {}", user.getEmail());
	}

	public String generateActivationCode( int length ) {
		if ( length < 1 ) {
			throw new IllegalArgumentException("Length must be greater than 0");
		}

		return IntStream.range(0, length).map(i -> secureRandom.nextInt(CHARACTERS.length())).mapToObj(CHARACTERS::charAt).map(String::valueOf).collect(Collectors.joining());
	}
}
