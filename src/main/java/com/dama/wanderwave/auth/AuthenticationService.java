package com.dama.wanderwave.auth;

import com.dama.wanderwave.email.EmailService;
import com.dama.wanderwave.handler.*;
import com.dama.wanderwave.role.RoleRepository;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.token.Token;
import com.dama.wanderwave.token.TokenRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthenticationService {

	private static final Integer RECOVERY_TOKEN_LENGTH = 9;
	private static final Integer ACTIVATION_TOKEN_LENGTH = 6;
	private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;
	private final RoleRepository roleRepository;
	private final TokenRepository tokenRepository;
	private final EmailService emailService;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public String register(RegistrationRequest request) {
		log.info("Starting registration process for user with username: {} and email: {}", request.getUsername(), request.getEmail());
		checkForExistingUser(request.getUsername(), request.getEmail());

		User user = createUser(request);
		log.info("New user created with nickname: {}", user.getNickname());

		User newUser = userRepository.save(user);
		log.info("User {} successfully saved to database with ID: {}", newUser.getNickname(), newUser.getId());

		sendValidationEmail(newUser);
		log.info("Registration process completed for user: {}", newUser.getNickname());
		return "Added new User::" + newUser.getId();
	}

	private void checkForExistingUser(String username, String email) {
		log.debug("Checking for existing user with username: {} or email: {}", username, email);
		userRepository.findByNicknameOrEmail(username, email).ifPresent(user -> {
			String field = user.getNickname().equals(username) ? "nickname" : "email";
			log.warn("Attempted registration with existing {}: {}", field, field.equals("nickname") ? username : email);
			throw new UniqueConstraintViolationException("Unique constraint violation: " + field, field);
		});
	}

	private User createUser(RegistrationRequest request) {
		log.debug("Creating new user with username: {}", request.getUsername());
		var userRole = roleRepository.findByName("USER")
				               .orElseThrow(() -> {
					               log.error("ROLE USER not found in the database");
					               return new RoleNotFoundException("ROLE USER was not initiated");
				               });

		return User.builder()
				       .nickname(request.getUsername())
				       .email(request.getEmail())
				       .description("")
				       .password(passwordEncoder.encode(request.getPassword()))
				       .accountLocked(false)
				       .enabled(false)
				       .roles(Set.of(userRole))
				       .build();
	}

	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		log.info("Attempting to authenticate user: {}", request.getEmail());
		Authentication auth;
		try {
			auth = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
			);
		} catch (Exception e) {
			log.warn("Authentication failed for user: {}. Reason: {}", request.getEmail(), e.getMessage());
			throw e;
		}

		var user = (User) auth.getPrincipal();
		log.info("User authenticated successfully: {}", user.getEmail());

		var claims = createClaims(user);
		var jwtToken = jwtService.generateToken(claims, user);
		log.debug("JWT token generated for user: {}", user.getEmail());

		return AuthenticationResponse.builder().token(jwtToken).build();
	}

	@Transactional
	public String activateAccount(String token) {
		log.info("Attempting to activate account with token: {}", token);
		Token savedToken = findTokenOrThrow(token);
		checkTokenExpiration(savedToken, this::sendValidationEmail);

		activateUser(savedToken.getUser());
		markTokenAsValidated(savedToken);

		log.info("Account activated successfully for user: {}", savedToken.getUser().getEmail());
		return savedToken.getContent();
	}

	public String recoverAccount(String email) {
		log.info("Initiating account recovery for email: {}", email);
		userRepository.findByEmail(email).ifPresentOrElse(
				this::sendRecoveryEmail,
				() -> log.warn("Recovery attempted for non-existent email: {}", email)
		);
		return "Message has been sent!";
	}

	@Transactional
	public ResponseRecord changeUserPassword(String token, String password) {
		log.info("Attempting to change password using token: {}", token);
		Token savedToken = findTokenOrThrow(token);
		checkTokenExpiration(savedToken, this::sendRecoveryEmail);

		changeUserPassword(savedToken.getUser(), password);
		markTokenAsValidated(savedToken);

		log.info("Password changed successfully for user: {}", savedToken.getUser().getEmail());
		return new ResponseRecord(202, "Password changed successfully");
	}

	protected void checkTokenExpiration( Token token, java.util.function.Consumer<User> resendAction ) {
		if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
			log.warn("Token expired for user: {}. Resending new token.", token.getUser().getEmail());
			resendAction.accept(token.getUser());
			throw new TokenExpiredException("Token has expired. A new token has been sent to the same email address.");
		}
	}

	protected Map<String, Object> createClaims( User user ) {
		var claims = new HashMap<String, Object>();
		claims.put("username", user.getNickname());
		log.debug("Created claims for user: {}", user.getEmail());
		return claims;
	}

	protected void sendValidationEmail( User user ) {
		log.info("Sending validation email to user: {}", user.getEmail());
		sendEmail(user, ACTIVATION_TOKEN_LENGTH, emailService::sendValidationEmail);
	}

	private void sendRecoveryEmail(User user) {
		log.info("Sending recovery email to user: {}", user.getEmail());
		sendEmail(user, RECOVERY_TOKEN_LENGTH, emailService::sendRecoveryEmail);
	}

	void sendEmail( User user, int tokenLength, EmailSender emailSender ) {
		try {
			String token = generateAndSaveToken(user, tokenLength);
			emailSender.sendEmail(token, user.getEmail());
			log.info("Email sent successfully to user: {}", user.getEmail());
		} catch (IOException e) {
			log.error("Error reading email template for user: {}. Error: {}", user.getEmail(), e.getMessage());
			throw new EmailTemplateException("Error reading email template");
		} catch (MessagingException e) {
			log.error("Error sending email to user: {}. Error: {}", user.getEmail(), e.getMessage());
			throw new EmailSendingException("Error sending email");
		}
	}

	protected Token findTokenOrThrow(String token) {
		log.debug("Searching for token: {}", token);
		return tokenRepository.findByContent(token)
				       .orElseThrow(() -> {
					       log.warn("Invalid token attempted: {}", token);
					       return new TokenNotFoundException("Invalid token");
				       });
	}

	protected void activateUser(User user) {
		user.setEnabled(true);
		userRepository.save(user);
		log.info("User account activated: {}", user.getEmail());
	}

	protected void changeUserPassword(User user, String password) {
		user.setPassword(passwordEncoder.encode(password));
		userRepository.save(user);
		log.info("Password changed for user: {}", user.getEmail());
	}

	protected void markTokenAsValidated(Token token) {
		token.setValidatedAt(LocalDateTime.now());
		tokenRepository.save(token);
		log.debug("Token marked as validated: {}", token.getContent());
	}

	protected String generateAndSaveToken(User user, int length) {
		String tokenContent = generateTokenCode(length);
		Token token = Token.builder()
				              .content(tokenContent)
				              .createdAt(LocalDateTime.now())
				              .expiresAt(LocalDateTime.now().plusMinutes(15))
				              .user(user)
				              .build();
		tokenRepository.save(token);
		log.debug("Generated and saved new token for user: {}", user.getEmail());
		return tokenContent;
	}

	public String generateTokenCode(int length) {
		if (length < 1) {
			log.error("Attempted to generate token with invalid length: {}", length);
			throw new IllegalArgumentException("Length must be greater than 0");
		}

		String token = secureRandom.ints(length, 0, CHARACTERS.length())
				               .mapToObj(CHARACTERS::charAt)
				               .map(String::valueOf)
				               .collect(Collectors.joining());
		log.debug("Generated token of length: {}", length);
		return token;
	}

	@FunctionalInterface
	protected interface EmailSender {
		void sendEmail(String token, String email) throws IOException, MessagingException;
	}
}