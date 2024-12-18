package com.dama.wanderwave.auth;

import com.dama.wanderwave.email.EmailService;
import com.dama.wanderwave.handler.token.TokenExpiredException;
import com.dama.wanderwave.handler.token.TokenNotFoundException;
import com.dama.wanderwave.handler.user.UniqueConstraintViolationException;
import com.dama.wanderwave.refreshToken.RefreshToken;
import com.dama.wanderwave.refreshToken.RefreshTokenService;
import com.dama.wanderwave.role.Role;
import com.dama.wanderwave.role.RoleRepository;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.emailToken.EmailToken;
import com.dama.wanderwave.emailToken.EmailTokenRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.*;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

	@InjectMocks
	private AuthenticationService authenticationService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private EmailTokenRepository emailTokenRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private EmailService emailService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private JwtService jwtService;

	@Mock
	private AuthenticationManager authenticationManager;

	private User mockUser;
	private EmailToken mockEmailToken;

	@BeforeEach
	void setUp() {
		mockUser = new User();
		mockUser.setEmail("test@example.com");
		mockUser.setNickname("testUser");
		mockUser.setImageUrl("http://example.com/image.jpg");

		mockEmailToken = EmailToken.builder()
				                 .content("mockToken")
				                 .createdAt(LocalDateTime.now())
				                 .expiresAt(LocalDateTime.now().plusMinutes(15))
				                 .user(mockUser)
				                 .build();

	}

	@Nested
	@DisplayName("findUserByEmail Method")
	class FindUserByEmailTests {

		@Test
		@DisplayName("Should return null with non-existent email")
		void findUserByEmailShouldReturnNullWithNonExistentEmail() {
			String mockEmail = "test@test.com";
			when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.empty());

			User user = userRepository.findByEmail(mockEmail).orElse(null);

			verify(userRepository, times(1)).findByEmail(mockEmail);
			assertNull(user);
		}

		@Test
		@DisplayName("Should return user with existent email")
		void findUserByEmailShouldReturnUserWithExistentEmail() {
			String mockEmail = "test@test.com";
			when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockUser));

			User user = userRepository.findByEmail(mockEmail).orElse(null);

			verify(userRepository, times(1)).findByEmail(mockEmail);
			assertSame(user, mockUser);
		}
	}

	@Nested
	@DisplayName("findUserById Method")
	class FindUserByIdTests {

		@Test
		@DisplayName("Should return null with non-existent id")
		void findUserByIdShouldReturnNullWithNonExistentId() {
			String mockId = "mockId";
			when(userRepository.findById(mockId)).thenReturn(Optional.empty());

			User user = userRepository.findById(mockId).orElse(null);

			verify(userRepository, times(1)).findById(mockId);
			assertNull(user);
		}

		@Test
		@DisplayName("Should return user with existent id")
		void findUserByIdShouldReturnUserWithExistentId() {
			String mockId = "mockId";
			when(userRepository.findById(mockId)).thenReturn(Optional.of(mockUser));

			User user = userRepository.findById(mockId).orElse(null);

			verify(userRepository, times(1)).findById(mockId);
			assertSame(user, mockUser);
		}
	}

	@Nested
	@DisplayName("findTokenByContent Method")
	class FindTokenByContentTests {

		@Test
		@DisplayName("Should return token when token exists")
		void findExistingTokenShouldBeOk() {
			String mockContent = "testToken";
			when(emailTokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockEmailToken));

			EmailToken emailToken = emailTokenRepository.findByContent(mockContent).orElse(null);

			verify(emailTokenRepository, times(1)).findByContent(mockContent);
			assertSame(mockEmailToken, emailToken);
		}

		@Test
		@DisplayName("Should return null when token does not exist")
		void findNonExistingTokenShouldBeNull() {
			String mockContent = "testToken";
			when(emailTokenRepository.findByContent(mockContent)).thenReturn(Optional.empty());

			EmailToken emailToken = emailTokenRepository.findByContent(mockContent).orElse(null);

			verify(emailTokenRepository, times(1)).findByContent(mockContent);
			assertNull(emailToken);
		}
	}

	@Nested
	@DisplayName("saveToken Method")
	class SaveTokenTests {

		@Test
		@DisplayName("Should save token successfully")
		void saveTokenShouldBeOk() {
			when(emailTokenRepository.save(mockEmailToken)).thenReturn(mockEmailToken);

			EmailToken emailToken = emailTokenRepository.save(mockEmailToken);

			verify(emailTokenRepository, times(1)).save(mockEmailToken);
			assertNotNull(emailToken);
			assertSame(mockEmailToken, emailToken);
		}
	}

	@Nested
	@DisplayName("recoverAccount Method")
	class RecoverAccountTests {

		@Test
		@DisplayName("Should not throw exception when user with non-existent email")
		void recoverUserWithNonExistentEmailShouldBeOk() {
			String mockEmail = "test@test.com";
			when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.empty());

			User user = userRepository.findByEmail(mockEmail).orElse(null);

			verify(userRepository, times(1)).findByEmail(mockEmail);
			assertNull(user);
			assertDoesNotThrow(() -> authenticationService.recoverAccount(mockEmail));
		}

		@Test
		@DisplayName("Should not throw exception when user with existent email")
		void recoverUserWithExistentEmailShouldBeOk() {
			String mockEmail = "test@test.com";
			when(userRepository.findByEmail(mockEmail)).thenReturn(Optional.of(mockUser));

			User user = userRepository.findByEmail(mockEmail).orElse(null);

			verify(userRepository, times(1)).findByEmail(mockEmail);
			assertSame(mockUser, user);
			assertDoesNotThrow(() -> authenticationService.recoverAccount(mockEmail));
		}
	}

	@Nested
	@DisplayName("generateTokenCode Method")
	class GenerateTokenCodeTests {

		@Test
		@DisplayName("Should generate token with normal length")
		void generateTokenCodeNormalCase() {
			int length = 6;
			String token = authenticationService.generateTokenCode(length);

			assertNotNull(token);
			assertEquals(length, token.length());
		}

		@Test
		@DisplayName("Should generate token with edge case length one")
		void generateTokenCodeEdgeCaseLengthOne() {
			int length = 1;
			String token = authenticationService.generateTokenCode(length);

			assertNotNull(token);
			assertEquals(length, token.length());
		}

		@Test
		@DisplayName("Should throw exception when length is zero")
		void generateTokenCodeInvalidCaseLengthZero() {
			int length = 0;

			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authenticationService.generateTokenCode(length));

			assertEquals("Length must be greater than 0", exception.getMessage());
		}

		@Test
		@DisplayName("Should throw exception when length is negative")
		void generateTokenCodeInvalidCaseLengthNegative() {
			int length = -5;

			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authenticationService.generateTokenCode(length));

			assertEquals("Length must be greater than 0", exception.getMessage());
		}

		@ParameterizedTest
		@ValueSource(ints = {2, 4, 8, 10})
		@DisplayName("Should generate token with different lengths")
		void generateTokenCodeDifferentLengths(int length) {
			String token = authenticationService.generateTokenCode(length);

			assertNotNull(token);
			assertEquals(length, token.length());
		}
	}

	@Nested
	@DisplayName("generateAndSaveToken Method")
	class GenerateAndSaveTokenTests {

		@Test
		@DisplayName("Should generate and save token successfully")
		void generateAndSaveTokenShouldGenerateTokenAndSaveIt() {
			int tokenLength = 10;
			String generatedToken = "mockedTokenContent";

			AuthenticationService spyService = spy(authenticationService);
			doReturn(generatedToken).when(spyService).generateTokenCode(tokenLength);

			String result = spyService.generateAndSaveToken(mockUser, tokenLength);

			assertEquals(generatedToken, result);

			ArgumentCaptor<EmailToken> tokenCaptor = ArgumentCaptor.forClass(EmailToken.class);
			verify(emailTokenRepository, times(1)).save(tokenCaptor.capture());
			EmailToken savedEmailToken = tokenCaptor.getValue();

			assertEquals(generatedToken, savedEmailToken.getContent());
			assertEquals(mockUser, savedEmailToken.getUser());
			assertNotNull(savedEmailToken.getCreatedAt());
			assertNotNull(savedEmailToken.getExpiresAt());
			assertTrue(savedEmailToken.getExpiresAt().isAfter(savedEmailToken.getCreatedAt()));
		}

		@Test
		@DisplayName("Should throw exception when length is invalid")
		void generateTokenCodeShouldThrowExceptionWhenLengthIsInvalid() {
			int invalidLength = 0;
			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authenticationService.generateTokenCode(invalidLength));
			assertEquals("Length must be greater than 0", exception.getMessage());
		}

		@Test
		@DisplayName("Should return valid token when length is valid")
		void generateTokenCodeShouldReturnValidTokenWhenLengthIsValid() {
			int validLength = 6;

			String token = authenticationService.generateTokenCode(validLength);

			assertNotNull(token);
			assertEquals(validLength, token.length());
		}
	}

	@Nested
	@DisplayName("markTokenAsValidated Method")
	class MarkTokenAsValidatedTests {

		@Test
		@DisplayName("Should set validatedAt and save token")
		void markTokenAsValidatedShouldSetValidatedAtAndSaveToken() {
			authenticationService.markTokenAsValidated(mockEmailToken);

			assertNotNull(mockEmailToken.getValidatedAt());
			assertTrue(mockEmailToken.getValidatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));

			verify(emailTokenRepository, times(1)).save(mockEmailToken);
		}

		@Test
		@DisplayName("Should throw exception when token is null")
		void markTokenAsValidatedShouldThrowExceptionWhenTokenIsNull() {
			assertThrows(NullPointerException.class, () -> authenticationService.markTokenAsValidated(null));

			verify(emailTokenRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("findTokenOrThrow Method")
	class FindTokenOrThrowTests {

		@Test
		@DisplayName("Should return token when token exists")
		void findTokenOrThrowShouldReturnTokenWhenTokenExists() {
			String tokenContent = "validToken";
			when(emailTokenRepository.findByContent(tokenContent)).thenReturn(Optional.of(mockEmailToken));

			EmailToken result = authenticationService.findTokenOrThrow(tokenContent);

			assertEquals(mockEmailToken, result);
		}

		@Test
		@DisplayName("Should throw exception when token does not exist")
		void findTokenOrThrowShouldThrowTokenNotFoundExceptionWhenTokenDoesNotExist() {
			String tokenContent = "invalidToken";
			when(emailTokenRepository.findByContent(tokenContent)).thenReturn(Optional.empty());

			TokenNotFoundException exception = assertThrows(TokenNotFoundException.class, () -> authenticationService.findTokenOrThrow(tokenContent));

			assertEquals("Invalid token", exception.getMessage());
		}
	}

	@Nested
	@DisplayName("activateUser Method")
	class ActivateUserTests {

		@Test
		@DisplayName("Should enable user and save")
		void activateUserShouldEnableUserAndSave() {
			mockUser.setEnabled(false);

			authenticationService.activateUser(mockUser);

			assertTrue(mockUser.isEnabled());
			verify(userRepository).save(mockUser);
		}
	}

	@Nested
	@DisplayName("changeUserPassword Method")
	class ChangeUserPasswordTests {

		@Test
		@DisplayName("Should fail with wrong token")
		void changePasswordWithWrongTokenShouldFail() {
			String mockToken = "mockToken";
			String mockPassword = "mockPassword";

			assertThrows(RuntimeException.class, () -> authenticationService.changeUserPassword(mockToken, mockPassword));
		}

		@Test
		@DisplayName("Should fail with expired token")
		void changePasswordWithExpiredTokenShouldFail() {
			String mockContent = "mockContent";
			mockEmailToken.setCreatedAt(LocalDateTime.now().minusMinutes(20));
			mockEmailToken.setExpiresAt(LocalDateTime.now().minusMinutes(5));

			when(emailTokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockEmailToken));

			assertThrows(TokenExpiredException.class, () -> authenticationService.changeUserPassword(mockContent, "mockPassword"));
		}

		@Test
		@DisplayName("Should succeed with correct token")
		void changePasswordWithCorrectTokenShouldBeOk() {
			String mockContent = "mockContent";
			when(emailTokenRepository.findByContent(mockContent)).thenReturn(Optional.of(mockEmailToken));

			assertDoesNotThrow(() -> authenticationService.changeUserPassword(mockContent, "mockPassword"));
		}
	}

	@Nested
	@DisplayName("register Method")
	class RegisterTests {

		@Test
		@DisplayName("Should create new user and send validation email")
		void registerShouldCreateNewUserAndSendValidationEmail() throws Exception {
			RegistrationRequest registrationRequest = new RegistrationRequest("testUser", "test@example.com", "password");
			Role role = Role.builder().name("USER").build();

			when(userRepository.findByNicknameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());
			when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
			when(userRepository.save(any(User.class))).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));

			String result = authenticationService.register(registrationRequest);

			assertNotNull(result);
			assertTrue(result.contains("Added new User::"));

			ArgumentCaptor<User> userArgumentCaptor = ArgumentCaptor.forClass(User.class);
			verify(userRepository, times(1)).save(userArgumentCaptor.capture());
			User savedUser = userArgumentCaptor.getValue();

			assertEquals("testUser", savedUser.getNickname());
			assertEquals("test@example.com", savedUser.getEmail());
			assertFalse(savedUser.isEnabled());
			assertEquals(Set.of(role), savedUser.getRoles());
			verify(emailService, times(1)).sendValidationEmail(anyString(), eq(savedUser.getEmail()));
		}

		@Test
		@DisplayName("Should throw exception when user with same nickname exists")
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
		@DisplayName("Should throw exception when user with same email exists")
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
		@DisplayName("Should throw exception when role not found")
		void registerShouldThrowExceptionWhenRoleNotFound() throws Exception {
			RegistrationRequest registrationRequest = new RegistrationRequest("testUser", "test@example.com", "password");
			when(userRepository.findByNicknameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());
			when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

			assertThrows(RuntimeException.class, () -> authenticationService.register(registrationRequest));

			verify(userRepository, never()).save(any(User.class));
			verify(emailService, never()).sendValidationEmail(anyString(), anyString());
		}
	}

	@Nested
	@DisplayName("authenticate Method")
	class AuthenticateTests {

		@Test
		@DisplayName("Should return token when credentials are correct")
		void authenticateShouldReturnTokenWhenCredentialsAreCorrect() {
			AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password");
			Authentication auth = mock(Authentication.class);
			when(auth.getPrincipal()).thenReturn(mockUser);
			when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

			Map<String, Object> claims = new HashMap<>();
			claims.put("id", mockUser.getId());
			claims.put("nickname", mockUser.getNickname());
			claims.put("email", mockUser.getEmail());
			claims.put("authorities", mockUser.getAuthorities());
			claims.put("imgUrl", mockUser.getImageUrl());

			String accessToken = "access";

			when(jwtService.generateToken(claims, mockUser)).thenReturn(accessToken);

			RefreshToken refreshToken = new RefreshToken();
			refreshToken.setToken("refresh");

			when(refreshTokenService.createRefreshToken(mockUser)).thenReturn(refreshToken);

			AuthenticationResponse response = authenticationService.authenticate(request);

			assertNotNull(response);
			assertEquals(accessToken, response.getAccessToken());
			assertEquals(refreshToken.getToken(), response.getRefreshToken());
			verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
			verify(jwtService, times(1)).generateToken(claims, mockUser);
		}

		@Test
		@DisplayName("Should throw exception when credentials are incorrect")
		void authenticateShouldThrowExceptionWhenCredentialsAreIncorrect() {
			AuthenticationRequest request = new AuthenticationRequest("test@example.com", "wrongPassword");
			when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new BadCredentialsException("Invalid credentials"));

			BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> authenticationService.authenticate(request));

			assertEquals("Invalid credentials", exception.getMessage());
			verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
			verify(jwtService, never()).generateToken(anyMap(), any(User.class));
		}

		@Test
		@DisplayName("Should throw exception when authentication manager throws exception")
		void authenticateShouldThrowExceptionWhenAuthenticationManagerThrowsException() {
			AuthenticationRequest request = new AuthenticationRequest("test@example.com", "password");
			when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(new RuntimeException("Authentication failed"));

			RuntimeException exception = assertThrows(RuntimeException.class, () -> authenticationService.authenticate(request));

			assertEquals("Authentication failed", exception.getMessage());
			verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
			verify(jwtService, never()).generateToken(anyMap(), any(User.class));
		}
	}

	@Nested
	@DisplayName("activateAccount Method")
	class ActivateAccountTests {

		@Test
		@DisplayName("Should activate account successfully")
		void activateAccount_ShouldActivateAccountSuccessfully() {
			String tokenContent = "validToken";
			User user = new User();
			user.setId("userId");
			user.setEmail("test@example.com");
			user.setEnabled(false);
			EmailToken emailToken = EmailToken.builder().content(tokenContent).createdAt(LocalDateTime.now()).expiresAt(LocalDateTime.now().plusMinutes(15)).user(user).build();

			when(emailTokenRepository.findByContent(tokenContent)).thenReturn(Optional.of(emailToken));

			String result = authenticationService.activateAccount(tokenContent);

			assertEquals(tokenContent, result);
			verify(userRepository, times(1)).save(user);
			verify(emailTokenRepository, times(1)).save(emailToken);
			assertTrue(user.isEnabled());
			assertNotNull(emailToken.getValidatedAt());
		}

		@Test
		@DisplayName("Should throw exception when token does not exist")
		void activateAccountShouldThrowTokenNotFoundExceptionWhenTokenDoesNotExist() {
			String tokenContent = "invalidToken";
			when(emailTokenRepository.findByContent(tokenContent)).thenReturn(Optional.empty());

			TokenNotFoundException exception = assertThrows(TokenNotFoundException.class, () -> authenticationService.activateAccount(tokenContent));

			assertEquals("Invalid token", exception.getMessage());
			verify(userRepository, never()).save(any(User.class));
			verify(emailTokenRepository, never()).save(any(EmailToken.class));
		}

		@Test
		@DisplayName("Should throw exception when token is expired")
		void activateAccountShouldThrowTokenExpiredExceptionWhenTokenIsExpired() {
			String tokenContent = "expiredToken";
			mockEmailToken.setCreatedAt(LocalDateTime.now().minusMinutes(20));
			mockEmailToken.setExpiresAt(LocalDateTime.now().minusMinutes(5));

			when(emailTokenRepository.findByContent(tokenContent)).thenReturn(Optional.of(mockEmailToken));

			TokenExpiredException exception = assertThrows(TokenExpiredException.class, () -> authenticationService.activateAccount(tokenContent));

			assertEquals("Token has expired. A new token has been sent to the same email address.", exception.getMessage());
			verify(userRepository, never()).save(any(User.class));
		}
	}

	@Nested
	@DisplayName("sendEmail Method")
	class SendEmailTests {

		@Test
		@DisplayName("Should send email successfully")
		void sendEmailShouldSendEmail() throws Exception {
			int tokenLength = 6;
			AuthenticationService.EmailSender emailSender = mock(AuthenticationService.EmailSender.class);

			doNothing().when(emailSender).sendEmail(anyString(), eq(mockUser.getEmail()));

			authenticationService.sendEmail(mockUser, tokenLength, emailSender);

			verify(emailSender, times(1)).sendEmail(anyString(), eq(mockUser.getEmail()));
		}
	}

	@Nested
	@DisplayName("checkTokenExpiration Method")
	class CheckTokenExpirationTests {

		@Test
		@DisplayName("Should not throw exception when token is not expired")
		void checkTokenExpirationShouldNotThrowExceptionWhenTokenIsNotExpired() {
			assertDoesNotThrow(() -> authenticationService.checkTokenExpiration(mockEmailToken, user1 -> { }));
		}

		@Test
		@DisplayName("Should throw exception when token is expired")
		void checkTokenExpirationShouldThrowExceptionWhenTokenIsExpired() {
			mockEmailToken.setCreatedAt(LocalDateTime.now().minusMinutes(20));
			mockEmailToken.setExpiresAt(LocalDateTime.now().minusMinutes(5));

			TokenExpiredException exception = assertThrows(TokenExpiredException.class, () -> authenticationService.checkTokenExpiration(mockEmailToken, user1 -> { }));

			assertEquals("Token has expired. A new token has been sent to the same email address.", exception.getMessage());
		}
	}
	@Nested
	@DisplayName("createClaims Method")
	class CreateClaimsTests {

		@Test
		@DisplayName("Should return correct claims")
		void createClaimsShouldReturnCorrectClaims() {

			Map<String, Object> claims = authenticationService.createClaims(mockUser);

			assertNotNull(claims);
			assertEquals("testUser", claims.get("nickname"));
		}
	}
}