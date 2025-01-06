package com.dama.wanderwave.auth;

import com.dama.wanderwave.handler.*;
import com.dama.wanderwave.handler.email.EmailSendingException;
import com.dama.wanderwave.handler.email.EmailTemplateException;
import com.dama.wanderwave.handler.token.TokenExpiredException;
import com.dama.wanderwave.handler.token.TokenNotFoundException;
import com.dama.wanderwave.handler.token.TokenRefreshException;
import com.dama.wanderwave.handler.user.UniqueConstraintViolationException;
import com.dama.wanderwave.handler.user.UserNotFoundException;
import com.dama.wanderwave.refreshToken.RefreshToken;
import com.dama.wanderwave.refreshToken.RefreshTokenService;
import com.dama.wanderwave.refreshToken.TokenRefreshRequest;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Objects;
import java.util.Optional;

import static com.dama.wanderwave.auth.ApiUrls.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Getter
@RequiredArgsConstructor
enum ApiUrls {
    RECOVER_ACCOUNT("/api/auth/recover-account"),
    CHANGE_PASSWORD("/api/auth/change-password"),
    REGISTER("/api/auth/register"),
    AUTHENTICATION("/api/auth/authenticate"),
    ACTIVE_ACCOUNT("/api/auth/activate-account"),
    REFRESH_TOKEN("/api/auth/refresh-token");

    private final String url;
}

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationController Tests")
class AuthenticationControllerTest {
    @InjectMocks
    private AuthenticationController authenticationController;
    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private JwtService jwtService;

    public record ErrorResponse(int errorCode, String message) {
    }

    public record ResponseRecord(int code, String message) {
    }

    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
    private static final MediaType ACCEPT_TYPE = MediaType.APPLICATION_JSON;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).setControllerAdvice(new GlobalExceptionHandler()).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("register Method")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void registerNewUserShouldBeOk() throws Exception {
            RegistrationRequest mockRequest = createMockRequest();
            String mockRequestJson = mapToJson(mockRequest);

            mockMvc.perform(post(REGISTER.getUrl()).contentType(CONTENT_TYPE).content(mockRequestJson)).andExpect(status().isAccepted());

            verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"username", "email"})
        @DisplayName("Should fail with conflict when user already exists")
        void registerUserWithExistingConstraintShouldFail(String conflictField) throws Exception {
            RegistrationRequest mockRequest = createMockRequest();
            String mockRequestJson = mapToJson(mockRequest);

            switch (conflictField) {
                case "username" ->
                        when(authenticationService.register(any(RegistrationRequest.class))).thenThrow(new UniqueConstraintViolationException("Unique constraint violation: username", "nickname"));
                case "email" ->
                        when(authenticationService.register(any(RegistrationRequest.class))).thenThrow(new UniqueConstraintViolationException("Unique constraint violation: email", "email"));
                default -> when(authenticationService.register(any(RegistrationRequest.class)));
            }

            mockMvc.perform(post(REGISTER.getUrl()).contentType(CONTENT_TYPE).content(mockRequestJson)).andExpect(status().isConflict());

            verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
        }

        @Test
        @DisplayName("Should fail when email sending fails")
        void registerNewUserShouldFailWhenEmailSendingFails() throws Exception {
            RegistrationRequest mockRequest = createMockRequest();
            String mockRequestJson = mapToJson(mockRequest);

            doThrow(new EmailSendingException("Failed to send email")).when(authenticationService).register(any(RegistrationRequest.class));

            mockMvc.perform(post(REGISTER.getUrl()).contentType(CONTENT_TYPE).content(mockRequestJson)).andExpect(status().isInternalServerError());

            verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
        }

        @Test
        @DisplayName("Should fail when email template fails")
        void registerNewUserShouldFailWhenEmailTemplateFails() throws Exception {
            RegistrationRequest mockRequest = createMockRequest();
            String mockRequestJson = mapToJson(mockRequest);

            doThrow(new EmailTemplateException("Failed to read email template")).when(authenticationService).register(any(RegistrationRequest.class));

            mockMvc.perform(post(REGISTER.getUrl()).contentType(CONTENT_TYPE).content(mockRequestJson)).andExpect(status().isInternalServerError());

            verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
        }
    }

    @Nested
    @DisplayName("authenticate Method")
    class AuthenticateTests {

        @Test
        @DisplayName("Should authenticate successfully and return tokens")
        void authenticateShouldReturnOkWithToken() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest("tahiheb432@sigmazon.com", "password");
            AuthenticationResponse response = new AuthenticationResponse("access", "refresh");

            when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(response);

            mockMvc.perform(post(AUTHENTICATION.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mapToJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(response.getAccessToken()))
                    .andExpect(jsonPath("$.refreshToken").value(response.getRefreshToken()));

            verify(authenticationService, times(1)).authenticate(any(AuthenticationRequest.class));
        }

        @Test
        @DisplayName("Should return unauthorized with bad credentials")
        void authenticateShouldReturnUnauthorizedWithUserUnauthorizedException() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest("tahiheb432@sigmazon.com", "password");
            ErrorResponse response = new ErrorResponse(401, "Bad credentials");

            when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenThrow(new BadCredentialsException(response.message));

            mockMvc.perform(post(AUTHENTICATION.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mapToJson(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));
        }

        @Test
        @DisplayName("Should return internal server error")
        void authenticateShouldReturnInternalServerError() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest("tahiheb432@sigmazon.com", "password");
            ErrorResponse response = new ErrorResponse(500, "Internal server error");

            when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenThrow(new RuntimeException(response.message));

            mockMvc.perform(post(AUTHENTICATION.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mapToJson(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).authenticate(any(AuthenticationRequest.class));
        }

        @Test
        @DisplayName("Should return bad request with invalid request format")
        void authenticateShouldReturnBadRequestWithInvalidRequestFormat() throws Exception {
            String invalidRequestJson = "{\"email\": \"tahiheb432@sigmazon.com\"}";

            mockMvc.perform(post(AUTHENTICATION.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(invalidRequestJson))
                    .andExpect(status().isBadRequest());

            verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
        }
    }

    @Nested
    @DisplayName("activeAccount Method")
    class ActiveAccountTests {

        @Test
        @DisplayName("Should activate account successfully")
        void activeAccountShouldReturnAccepted() throws Exception {
            String testActivationToken = "12345678";
            ResponseRecord response = new ResponseRecord(HttpStatus.ACCEPTED.value(), "test-token");

            when(authenticationService.activateAccount(testActivationToken)).thenReturn(response.message);

            mockMvc.perform(post(ACTIVE_ACCOUNT.getUrl())
                            .param("emailToken", testActivationToken)
                            .accept(ACCEPT_TYPE)
                            .content(CONTENT_TYPE))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.code").value(response.code))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).activateAccount(testActivationToken);
        }

        @Test
        @DisplayName("Should return not found with token not found exception")
        void activeAccountShouldReturnNotFoundWithTokenNotFoundException() throws Exception {
            String testActivationToken = "12345678";
            ErrorResponse response = new ErrorResponse(404, "Test Response Message");

            when(authenticationService.activateAccount(testActivationToken)).thenThrow(new TokenNotFoundException(response.message));

            mockMvc.perform(post(ACTIVE_ACCOUNT.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .param("emailToken", testActivationToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).activateAccount(testActivationToken);
        }

        @Test
        @DisplayName("Should return bad request with token expired exception")
        void activeAccountShouldReturnBadRequestWithTokenExpiredException() throws Exception {
            String testActivationToken = "12345678";
            ErrorResponse response = new ErrorResponse(400, "Bad Request");

            when(authenticationService.activateAccount(testActivationToken)).thenThrow(new TokenExpiredException(response.message));

            mockMvc.perform(post(ACTIVE_ACCOUNT.getUrl())
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE)
                            .param("emailToken", testActivationToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).activateAccount(testActivationToken);
        }

        @Test
        @DisplayName("Should return not found with user not found exception")
        void activeAccountShouldReturnNotFoundWithUserNotFoundException() throws Exception {
            String testActivationToken = "12345678";
            ErrorResponse response = new ErrorResponse(404, "Username not found");

            when(authenticationService.activateAccount(testActivationToken)).thenThrow(new UserNotFoundException(response.message));

            mockMvc.perform(post(ACTIVE_ACCOUNT.getUrl())
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE)
                            .param("emailToken", testActivationToken))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).activateAccount(testActivationToken);
        }

        @Test
        @DisplayName("Should return internal server error")
        void activeAccountShouldReturnInternalServerError() throws Exception {
            String testActivationToken = "12345678";
            ErrorResponse response = new ErrorResponse(500, "Internal Server Error");

            when(authenticationService.activateAccount(testActivationToken)).thenThrow(new InternalError(response.message));

            mockMvc.perform(post(ACTIVE_ACCOUNT.getUrl())
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE)
                            .param("emailToken", testActivationToken))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).activateAccount(testActivationToken);
        }
    }

    @Nested
    @DisplayName("recoverByEmail Method")
    class RecoverByEmailTests {

        @Test
        @DisplayName("Should recover account by email successfully")
        void recoverByEmailShouldReturnOk() throws Exception {
            String email = "temp@mail.com";
            ResponseRecord response = new ResponseRecord(HttpStatus.ACCEPTED.value(), "Message have " +
                    "sent!");

            when(authenticationService.recoverAccount(email)).thenReturn(response.message);

            mockMvc.perform(MockMvcRequestBuilders.get(RECOVER_ACCOUNT.getUrl())
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE)
                            .param("email", email))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.code").value(response.code))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).recoverAccount(email);
        }

        @Test
        @DisplayName("Should return internal server error")
        void recoverByEmailShouldReturnInternalError() throws Exception {
            String email = "temp@mail.com";
            ErrorResponse errorResponse = new ErrorResponse(500, "Internal Server Error");

            when(authenticationService.recoverAccount(email)).thenThrow(new RuntimeException("Internal Server Error"));

            mockMvc.perform(MockMvcRequestBuilders.get(RECOVER_ACCOUNT.getUrl())
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE)
                            .param("email", email))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(errorResponse.errorCode))
                    .andExpect(jsonPath("$.message").value(errorResponse.message));

            verify(authenticationService, times(1)).recoverAccount(email);
        }

        @Test
        @DisplayName("Should return internal server error with email template exception")
        void recoverByEmailShouldReturnInternalErrorWithEmailTemplateException() throws Exception {
            String email = "temp@mail.com";
            ErrorResponse errorResponse = new ErrorResponse(500, "Error reading recovery email template");

            when(authenticationService.recoverAccount(email)).thenThrow(new EmailTemplateException(errorResponse.message));

            mockMvc.perform(MockMvcRequestBuilders.get(RECOVER_ACCOUNT.getUrl())
                            .accept(ACCEPT_TYPE)
                            .contentType(CONTENT_TYPE)
                            .param("email", email))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value(errorResponse.errorCode))
                    .andExpect(jsonPath("$.message").value(errorResponse.message));

            verify(authenticationService, times(1)).recoverAccount(email);
        }
    }

    @Nested
    @DisplayName("changePassword Method")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void changePasswordShouldReturnOk() throws Exception {
            ResponseRecord response = new ResponseRecord(HttpStatus.OK.value(), "Password " +
                    "changed successfully");
            RecoveryRequest request = new RecoveryRequest("new_Password");

            when(authenticationService.changeUserPassword(anyString(), anyString())).thenReturn(new com.dama.wanderwave.utils.ResponseRecord(HttpStatus.OK.value(), "Password changed successfully"));

            mockMvc.perform(MockMvcRequestBuilders.post(CHANGE_PASSWORD.getUrl())
                            .accept(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer sigma-tkn")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(response.code))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).changeUserPassword(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw token not found exception")
        void changePasswordShouldThrowTokenNotFoundException() throws Exception {
            ErrorResponse response = new ErrorResponse(404, "User not found");

            var mockRecoveryRequest = RecoveryRequest
                    .builder()
                    .password("password")
                    .build();

            doThrow(new TokenNotFoundException(response.message))
                    .when(authenticationService).changeUserPassword(anyString(), anyString());

            mockMvc.perform(post(CHANGE_PASSWORD.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mapToJson(mockRecoveryRequest))
                            .header("Authorization", "Bearer sigma-tkn"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                    .andExpect(jsonPath("$.message").value(response.message));

            verify(authenticationService, times(1)).changeUserPassword(eq("sigma-tkn"), anyString());
        }


        @Test
        @DisplayName("Should throw when exception occurs")
        void changePasswordShouldThrowWhenExceptionOccurs() throws Exception {
            var mockRecoveryRequest = RecoveryRequest
                    .builder()
                    .password("password")
                    .build();

            doThrow(new RuntimeException())
                    .when(authenticationService).changeUserPassword(anyString(), anyString());

            mockMvc.perform(post(CHANGE_PASSWORD.getUrl())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapToJson(mockRecoveryRequest))
                            .header("Authorization", "Bearer sigma-tkn"))
                    .andExpect(status().isInternalServerError());

            verify(authenticationService, times(1)).changeUserPassword(eq("sigma-tkn"), anyString());
        }

    }

    @Nested
    @DisplayName("refreshToken Method")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should return new access token")
        void refreshTokenShouldReturnNewAccessToken() throws Exception {
            String refreshToken = "validRefreshToken";
            String newAccessToken = "newAccessToken";
            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);
            RefreshToken refreshTokenEntity = new RefreshToken();
            User user = new User();
            user.setNickname("testUser");
            refreshTokenEntity.setUser(user);
            AuthenticationResponse response = new AuthenticationResponse(newAccessToken, refreshToken);

            when(refreshTokenService.findByToken(refreshToken)).thenReturn(Optional.of(refreshTokenEntity));
            when(refreshTokenService.verifyExpiration(refreshTokenEntity)).thenReturn(refreshTokenEntity);
            when(jwtService.generateToken(anyMap(), eq(user))).thenReturn(newAccessToken);

            mockMvc.perform(post(REFRESH_TOKEN.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mapToJson(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value(response.getAccessToken()))
                    .andExpect(jsonPath("$.refreshToken").value(refreshToken));

            verify(refreshTokenService, times(1)).findByToken(refreshToken);
            verify(refreshTokenService, times(1)).verifyExpiration(refreshTokenEntity);
            verify(jwtService, times(1)).generateToken(anyMap(), eq(user));
        }

        @Test
        @DisplayName("Should return bad request for invalid refresh token")
        void refreshTokenShouldReturnBadRequestForInvalidRefreshToken() throws Exception {
            String refreshToken = "invalidRefreshToken";
            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);

            when(refreshTokenService.findByToken(refreshToken)).thenReturn(Optional.empty());

            mockMvc.perform(post(REFRESH_TOKEN.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mapToJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertInstanceOf(TokenRefreshException.class, result.getResolvedException()))
                    .andExpect(result -> assertEquals("Refresh token is invalid!", Objects.requireNonNull(result.getResolvedException()).getMessage()));

            verify(refreshTokenService, times(1)).findByToken(refreshToken);
            verify(refreshTokenService, never()).verifyExpiration(any());
            verify(jwtService, never()).generateToken(anyMap(), any());
        }

        @Test
        @DisplayName("Should return bad request for expired refresh token")
        void refreshTokenShouldReturnBadRequestForExpiredRefreshToken() throws Exception {
            String refreshToken = "expiredRefreshToken";
            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);
            RefreshToken refreshTokenEntity = new RefreshToken();

            when(refreshTokenService.findByToken(refreshToken)).thenReturn(Optional.of(refreshTokenEntity));
            when(refreshTokenService.verifyExpiration(refreshTokenEntity)).thenThrow(new TokenRefreshException("Refresh token is expired!"));

            mockMvc.perform(post(REFRESH_TOKEN.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mapToJson(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> assertTrue(result.getResolvedException() instanceof TokenRefreshException))
                    .andExpect(result -> assertEquals("Refresh token is expired!", result.getResolvedException().getMessage()));

            verify(refreshTokenService, times(1)).findByToken(refreshToken);
            verify(refreshTokenService, times(1)).verifyExpiration(refreshTokenEntity);
            verify(jwtService, never()).generateToken(anyMap(), any());
        }

        @Test
        @DisplayName("Should return internal server error")
        void refreshTokenShouldReturnInternalServerError() throws Exception {
            String refreshToken = "validRefreshToken";
            TokenRefreshRequest request = new TokenRefreshRequest(refreshToken);
            RefreshToken refreshTokenEntity = new RefreshToken();

            when(refreshTokenService.findByToken(refreshToken)).thenReturn(Optional.of(refreshTokenEntity));
            when(refreshTokenService.verifyExpiration(refreshTokenEntity)).thenThrow(new RuntimeException("Internal server error"));

            mockMvc.perform(post(REFRESH_TOKEN.getUrl())
                            .contentType(CONTENT_TYPE)
                            .accept(ACCEPT_TYPE)
                            .content(mapToJson(request)))
                    .andExpect(status().isInternalServerError());

            verify(refreshTokenService, times(1)).findByToken(refreshToken);
            verify(refreshTokenService, times(1)).verifyExpiration(refreshTokenEntity);
            verify(jwtService, never()).generateToken(anyMap(), any());
        }
    }

    private String mapToJson(Object request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private RegistrationRequest createMockRequest() {
        return RegistrationRequest.builder()
                .username("username")
                .password("Password1!")
                .email("email@gmail.com")
                .build();
    }
}
