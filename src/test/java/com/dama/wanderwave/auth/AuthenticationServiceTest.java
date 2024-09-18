package com.dama.wanderwave.auth;

import com.dama.wanderwave.email.EmailService;
import com.dama.wanderwave.handler.TokenExpiredException;
import com.dama.wanderwave.handler.TokenNotFoundException;
import com.dama.wanderwave.handler.UniqueConstraintViolationException;
import com.dama.wanderwave.role.Role;
import com.dama.wanderwave.role.RoleRepository;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.token.Token;
import com.dama.wanderwave.token.TokenRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {


	@InjectMocks
	private AuthenticationService authenticationService;
	@Mock
	private UserRepository userRepository;
	@Mock
	private TokenRepository tokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private EmailService emailService;


	@Mock
	private RoleRepository roleRepository;


	@Mock
	private JwtService jwtService;

	@Mock
	private AuthenticationManager authenticationManager;


	// findUserByEmail
	// ----------------------------------------------------------

	@Test
	void findUserByEmailShouldReturnNullWithNonExistentEmail() {
		String mockEmail = "test@test.com";
		when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.empty());

		User user = userRepository.findByEmail(mockEmail).orElse(null);

		verify(userRepository, times(1)).findByEmail(mockEmail);
		assertNull(user);
	}

	@Test
	void findUserByEmailShouldReturnUserWithExistentEmail() {
		String mockEmail = "test@test.com";
		User mockUser = new User();
		when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockUser));

		User user = userRepository.findByEmail(mockEmail).orElse(null);

		verify(userRepository, times(1)).findByEmail(mockEmail);
		assertSame(user, mockUser);
	}

	// findUserById
	// ----------------------------------------------------------
	@Test
	void findUserByIdShouldReturnNullWithNonExistentId() {
		String mockId = "mockId";
		when(userRepository.findById(mockId)).thenReturn(Optional.empty());

		User user = userRepository.findById(mockId).orElse(null);

		verify(userRepository, times(1)).findById(mockId);
		assertNull(user);
	}

	@Test
	void findUserByIdShouldReturnUserWithExistentId() {
		String mockId = "mockId";
		User mockUser = new User();
		when(userRepository.findById(mockId)).thenReturn(Optional.of(mockUser));

		User user = userRepository.findById(mockId).orElse(null);

		verify(userRepository, times(1)).findById(mockId);
		assertSame(user, mockUser);
	}


	// findTokenByContent
	// ----------------------------------------------------------
	@Test
	void findExistingTokenShouldBeOk() {
		String mockContent = "testToken";
		Token mockToken = new Token();
		when(tokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockToken));

		Token token = tokenRepository.findByContent(mockContent).orElse(null);

		verify(tokenRepository, times(1)).findByContent(mockContent);
		assertSame(mockToken, token);
	}

	@Test
	void findNonExistingTokenShouldBeNull() {
		String mockContent = "testToken";
		when(tokenRepository.findByContent(mockContent)).thenReturn(Optional.empty());

		Token token = tokenRepository.findByContent(mockContent).orElse(null);

		verify(tokenRepository, times(1)).findByContent(mockContent);
		assertNull(token);
	}

	// saveToken method
	// ----------------------------------------------------------
	@Test
	void saveTokenShouldBeOk() {
		Token mockToken = Token.builder().content("mockToken").createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(15)).user(new User()).build();

		when(tokenRepository.save(mockToken)).thenReturn(mockToken);

		Token token = tokenRepository.save(mockToken);

		verify(tokenRepository, times(1)).save(mockToken);
		assertNotNull(token);
		assertSame(mockToken, token);
	}


	// recoverAccount method
	// ----------------------------------------------------------
	@Test
	void recoverUserWithNonExistentEmailShouldBeOk() {
		String mockEmail = "test@test.com";
		when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.empty());

		User user = userRepository.findByEmail(mockEmail).orElse(null);

		verify(userRepository, times(1)).findByEmail(mockEmail);
		assertNull(user);
		assertDoesNotThrow(() -> authenticationService.recoverAccount(mockEmail));
	}

	@Test
	void recoverUserWithExistentEmailShouldBeOk() {
		String mockEmail = "test@test.com";
		User mockUser = new User();
		when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockUser));

		User user = userRepository.findByEmail(mockEmail).orElse(null);

		verify(userRepository, times(1)).findByEmail(mockEmail);
		assertSame(mockUser, user);
		assertDoesNotThrow(() -> authenticationService.recoverAccount(mockEmail));
	}

	// generateTokenCode method
	// ----------------------------------------------------------
	@Test
	void generateTokenCodeNormalCase() {
		int length = 6;
		String token = authenticationService.generateTokenCode(length);

		assertNotNull(token);
		assertEquals(length, token.length());
	}

	@Test
	void generateTokenCodeEdgeCaseLengthOne() {
		int length = 1;
		String token = authenticationService.generateTokenCode(length);

		assertNotNull(token);
		assertEquals(length, token.length());
	}

	@Test
	void generateTokenCodeInvalidCaseLengthZero() {
		int length = 0;

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authenticationService.generateTokenCode(length));

		assertEquals("Length must be greater than 0", exception.getMessage());
	}

	@Test
	void generateTokenCodeInvalidCaseLengthNegative() {
		int length = -5;

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authenticationService.generateTokenCode(length));

		assertEquals("Length must be greater than 0", exception.getMessage());
	}

	@Test
	void generateTokenCodeDifferentLengths() {
		int[] lengths = {2, 4, 8, 10};

		for ( int length : lengths ) {
			String token = authenticationService.generateTokenCode(length);

			assertNotNull(token);
			assertEquals(length, token.length());
		}
	}


	// generateAndSaveToken method
	// ----------------------------------------------------------

	@Test
	void generateAndSaveTokenShouldGenerateTokenAndSaveIt() {

		User user = new User();
		user.setEmail("test@example.com");

		int tokenLength = 10;
		String generatedToken = "mockedTokenContent";

		AuthenticationService spyService = spy(authenticationService);
		doReturn(generatedToken).when(spyService).generateTokenCode(tokenLength);


		String result = spyService.generateAndSaveToken(user, tokenLength);


		assertEquals(generatedToken, result);

		ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
		verify(tokenRepository, times(1)).save(tokenCaptor.capture());
		Token savedToken = tokenCaptor.getValue();

		assertEquals(generatedToken, savedToken.getContent());
		assertEquals(user, savedToken.getUser());
		assertNotNull(savedToken.getCreatedAt());
		assertNotNull(savedToken.getExpiresAt());
		assertTrue(savedToken.getExpiresAt().isAfter(savedToken.getCreatedAt()));

	}

	@Test
	void generateTokenCodeShouldThrowExceptionWhenLengthIsInvalid() {

		int invalidLength = 0;
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authenticationService.generateTokenCode(invalidLength));
		assertEquals("Length must be greater than 0", exception.getMessage());
	}

	@Test
	void generateTokenCodeShouldReturnValidTokenWhenLengthIsValid() {
		int validLength = 6;

		String token = authenticationService.generateTokenCode(validLength);

		assertNotNull(token);
		assertEquals(validLength, token.length());

	}
	// markTokenAsValidated method
	// ----------------------------------------------------------

	@Test
	void markTokenAsValidatedShouldSetValidatedAtAndSaveToken() {
		Token token = new Token();
		token.setContent("testToken");

		authenticationService.markTokenAsValidated(token);

		assertNotNull(token.getValidatedAt());
		assertTrue(token.getValidatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));

		verify(tokenRepository, times(1)).save(token);
	}

	@Test
	void markTokenAsValidatedShouldThrowExceptionWhenTokenIsNull() {
		assertThrows(NullPointerException.class, () -> authenticationService.markTokenAsValidated(null));

		verify(tokenRepository, never()).save(any());
	}


	// 	findTokenOrThrow method
	// ----------------------------------------------------------

	@Test
	void findTokenOrThrowShouldReturnTokenWhenTokenExists() {
		String tokenContent = "validToken";
		Token token = new Token();
		token.setContent(tokenContent);
		when(tokenRepository.findByContent(tokenContent)).thenReturn(Optional.of(token));

		Token result = authenticationService.findTokenOrThrow(tokenContent);

		assertEquals(token, result);
	}

	@Test
	void findTokenOrThrowShouldThrowTokenNotFoundExceptionWhenTokenDoesNotExist() {
		String tokenContent = "invalidToken";
		when(tokenRepository.findByContent(tokenContent)).thenReturn(Optional.empty());

		TokenNotFoundException exception = assertThrows(TokenNotFoundException.class, () -> authenticationService.findTokenOrThrow(tokenContent));

		assertEquals("Invalid token", exception.getMessage());
	}

	// 	activateUser method
	// ----------------------------------------------------------

	@Test
	void activateUserShouldEnableUserAndSave() {
		User user = new User();
		user.setEmail("test@example.com");
		user.setEnabled(false);

		authenticationService.activateUser(user);

		assertTrue(user.isEnabled());
		verify(userRepository).save(user);
	}


	// 	changeUserPassword method
	// ----------------------------------------------------------

	@Test
	void changePasswordWithWrongTokenShouldFail() {
		String mockToken = "mockToken";
		String mockPassword = "mockPassword";

		assertThrows(RuntimeException.class, () -> authenticationService.changeUserPassword(mockToken, mockPassword));
	}

	@Test
	void changePasswordWithExpiredTokenShouldFail() {
		String mockContent = "mockContent";
		Token mockToken = Token.builder().content(mockContent).createdAt(LocalDateTime.now().minusMinutes(20)).expiresAt(LocalDateTime.now().minusMinutes(5)).user(new User()).build();
		String mockPassword = "mockPassword";

		when(tokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockToken));

		assertThrows(TokenExpiredException.class, () -> authenticationService.changeUserPassword(mockContent, mockPassword));
	}

	@Test
	void changePasswordWithCorrectTokenShouldBeOk() {
		String mockContent = "mockContent";
		User mockUser = new User();
		mockUser.setId("mockId");
		Token mockToken = Token.builder().content(mockContent).createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(15)).user(mockUser).build();
		String mockPassword = "mockPassword";


		when(tokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockToken));

		assertDoesNotThrow(() -> authenticationService.changeUserPassword(mockContent, mockPassword));
	}

	// 	register method
	// ----------------------------------------------------------

	@Test
	void registerShouldCreateNewUserAndSendValidationEmail() throws Exception {
		RegistrationRequest registrationRequest = new RegistrationRequest("testUser", "text@example.com", "password");
		Role role = Role.builder().name("USER").build();

		when(userRepository.findByNicknameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());
		when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
		when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

		String result = authenticationService.register(registrationRequest);

		assertNotNull(result);
		assertTrue(result.contains("Added new User::"));

		ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository, times(1)).save(userArgumentCaptor.capture());
		User savedUser = userArgumentCaptor.getValue();

		assertEquals("testUser", savedUser.getNickname());
		assertEquals("text@example.com", savedUser.getEmail());
		assertFalse(savedUser.isEnabled());
		assertEquals(Set.of(role), savedUser.getRoles());
		verify(emailService, times(1)).sendValidationEmail(anyString(), eq(savedUser.getEmail()));
	}

	@Test
	void registerShouldThrowExceptionWhenUserWithSameNicknameExists() throws Exception {
		RegistrationRequest registrationRequest = new RegistrationRequest("testUser", "test@example.com", "password");
		User existingUser = new User();
		existingUser.setNickname("testUser");
		existingUser.setEmail("another@example.com");

		when(userRepository.findByNicknameOrEmail("testUser", "test@example.com")).thenReturn(Optional.of(existingUser));

		UniqueConstraintViolationException exception = assertThrows(UniqueConstraintViolationException.class, () -> authenticationService.register(registrationRequest));

		assertEquals("Unique constraint violation: nickname", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
		verify(emailService, never()).sendValidationEmail(anyString(), anyString());
	}

	@Test
	void registerShouldThrowExceptionWhenUserWithSameEmailExists() throws Exception {
		RegistrationRequest registrationRequest = new RegistrationRequest("testUser", "test@example.com", "password");
		User existingUser = new User();
		existingUser.setNickname("anotherUser");
		existingUser.setEmail("test@example.com");

		when(userRepository.findByNicknameOrEmail("testUser", "test@example.com")).thenReturn(Optional.of(existingUser));

		UniqueConstraintViolationException exception = assertThrows(UniqueConstraintViolationException.class, () -> authenticationService.register(registrationRequest));

		assertEquals("Unique constraint violation: email", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
		verify(emailService, never()).sendValidationEmail(anyString(), anyString());
	}

	@Test
	void registerShouldThrowExceptionWhenRoleNotFound() throws Exception {
		RegistrationRequest registrationRequest = new RegistrationRequest("testUser", "test@example.com", "password");
		when(userRepository.findByNicknameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());
		when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> authenticationService.register(registrationRequest));

		verify(userRepository, never()).save(any(User.class));
		verify(emailService, never()).sendValidationEmail(anyString(), anyString());
	}

	// 	authenticate method
	// ----------------------------------------------------------

	@Test
	void authenticate_ShouldReturnToken_WhenCredentialsAreCorrect() {
		AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password");
		User user = new User();
		user.setEmail("test@example.com");
		user.setNickname("testUser");

		Authentication auth = mock(Authentication.class);
		when(auth.getPrincipal()).thenReturn(user);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

		Map<String, Object> claims = new HashMap<>();
		claims.put("username", user.getNickname());
		String jwtToken = "mockJwtToken";
		when(jwtService.generateToken(claims, user)).thenReturn(jwtToken);


		AuthenticationResponse response = authenticationService.authenticate(request);


		assertNotNull(response);
		assertEquals(jwtToken, response.getToken());
		verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(jwtService, times(1)).generateToken(claims, user);
	}

	@Test
	void authenticateShouldThrowExceptionWhenCredentialsAreIncorrect() {
		AuthenticationRequest request = new AuthenticationRequest("test@example.com", "wrongPassword");
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

		BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));

		assertEquals("Invalid credentials", exception.getMessage());
		verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(jwtService, never()).generateToken(anyMap(), any(User.class));
	}

	@Test
	void authenticateShouldThrowExceptionWhenAuthenticationManagerThrowsException() {
		AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password");
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new RuntimeException("Authentication failed"));


		RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.authenticate(request));

		assertEquals("Authentication failed", exception.getMessage());
		verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(jwtService, never()).generateToken(anyMap(), any(User.class));
	}


	//  activateAccount method
	// ----------------------------------------------------------

	@Test
	void activateAccount_ShouldActivateAccountSuccessfully() {
		String tokenContent = "validToken";
		User user = new User();
		user.setId("userId");
		user.setEmail("test@example.com");
		user.setEnabled(false);
		Token token = Token.builder().content(tokenContent).createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(15)).user(user).build();

		when(tokenRepository.findByContent(tokenContent)).thenReturn(Optional.of(token));

		String result = authenticationService.activateAccount(tokenContent);

		assertEquals(tokenContent, result);
		verify(userRepository, times(1)).save(user);
		verify(tokenRepository, times(1)).save(token);
		assertTrue(user.isEnabled());
		assertNotNull(token.getValidatedAt());
	}

	@Test
	void activateAccountShouldThrowTokenNotFoundExceptionWhenTokenDoesNotExist() {

		String tokenContent = "invalidToken";
		when(tokenRepository.findByContent(tokenContent)).thenReturn(Optional.empty());


		TokenNotFoundException exception = assertThrows(TokenNotFoundException.class, () -> authenticationService.activateAccount(tokenContent));

		assertEquals("Invalid token", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
		verify(tokenRepository, never()).save(any(Token.class));
	}


	@Test
	void activateAccountShouldThrowTokenExpiredExceptionWhenTokenIsExpired() {
		String tokenContent = "expiredToken";
		User user = new User();
		user.setId("userId");
		user.setEmail("test@example.com");
		user.setEnabled(false);
		Token token = Token.builder().content(tokenContent).createdAt(LocalDateTime.now().minusMinutes(20)).expiresAt(LocalDateTime.now().minusMinutes(5)).user(user).build();

		when(tokenRepository.findByContent(tokenContent)).thenReturn(Optional.of(token));


		TokenExpiredException exception = assertThrows(TokenExpiredException.class, () -> authenticationService.activateAccount(tokenContent));

		assertEquals("Token has expired. A new token has been sent to the same email address.", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
	}

	// sendEmail method
	// ----------------------------------------------------------
	@Test
	void sendEmailShouldSendEmail() throws Exception {
		User user = new User();
		user.setEmail("test@example.com");
		int tokenLength = 6;
		AuthenticationService.EmailSender emailSender = mock(AuthenticationService.EmailSender.class);

		doNothing().when(emailSender).sendEmail(anyString(), eq(user.getEmail()));

		authenticationService.sendEmail(user, tokenLength, emailSender);

		verify(emailSender, times(1)).sendEmail(anyString(), eq(user.getEmail()));
	}

	// checkTokenExpiration method
	// ----------------------------------------------------------
	@Test
	void checkTokenExpirationShouldNotThrowExceptionWhenTokenIsNotExpired() {
		User user = new User();
		user.setEmail("test@example.com");
		Token token = Token.builder().content("validToken").createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(15)).user(user).build();

		assertDoesNotThrow(() -> authenticationService.checkTokenExpiration(token, user1 -> { }));
	}

	@Test
	void checkTokenExpirationShouldThrowExceptionWhenTokenIsExpired() {
		User user = new User();
		user.setEmail("test@example.com");
		Token token = Token.builder().content("expiredToken").createdAt(LocalDateTime.now().minusMinutes(20)).expiresAt(LocalDateTime.now().minusMinutes(5)).user(user).build();

		TokenExpiredException exception = assertThrows(TokenExpiredException.class, () -> authenticationService.checkTokenExpiration(token, user1 -> { }));

		assertEquals("Token has expired. A new token has been sent to the same email address.", exception.getMessage());
	}

	// createClaims method
	// ----------------------------------------------------------
	@Test
	void createClaimsShouldReturnCorrectClaims() {
		User user = new User();
		user.setNickname("testUser");
		user.setEmail("test@example.com");

		Map<String, Object> claims = authenticationService.createClaims(user);

		assertNotNull(claims);
		assertEquals("testUser", claims.get("username"));
	}

}