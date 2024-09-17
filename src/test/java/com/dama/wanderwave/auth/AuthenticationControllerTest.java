package com.dama.wanderwave.auth;


import com.dama.wanderwave.handler.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.dama.wanderwave.auth.ApiUrls.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.Getter;

@Getter
@RequiredArgsConstructor
enum ApiUrls {
    RECOVER_ACCOUNT("/api/auth/recover-account"),
    CHANGE_PASSWORD("/api/auth/change-password"),
    REGISTER("/api/auth/register"),
    AUTHENTICATION("/api/auth/authenticate"),
    ACTIVE_ACCOUNT("/api/auth/activate-account");


    private final String url;
}




@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {
    @InjectMocks
    private AuthenticationController authenticationController;
    @Mock
    private AuthenticationService authenticationService;


    public record ErrorResponse(int errorCode, String message) { }

	public record ResponseRecord (int code, String message) { }



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


    // register method
    // -------------------------------------------------------------------------------------

    @Test
    void registerNewUserShouldBeOk() throws Exception {
        RegistrationRequest mockRequest = createMockRequest();
        String mockRequestJson = mapToJson(mockRequest);

        mockMvc.perform(post(REGISTER.getUrl()).contentType(CONTENT_TYPE).content(mockRequestJson)).andExpect(status().isAccepted());

        verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email"})
    void registerUserWithExistingConstraintShouldFail( String conflictField ) throws Exception {
        RegistrationRequest mockRequest = createMockRequest();
        String mockRequestJson = mapToJson(mockRequest);

        switch ( conflictField ) {
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
    void registerNewUserShouldFailWhenEmailSendingFails() throws Exception {
        RegistrationRequest mockRequest = createMockRequest();
        String mockRequestJson = mapToJson(mockRequest);

        doThrow(new EmailSendingException("Failed to send email")).when(authenticationService).register(any(RegistrationRequest.class));

        mockMvc.perform(post(REGISTER.getUrl()).contentType(CONTENT_TYPE).content(mockRequestJson)).andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
    }

    @Test
    void registerNewUserShouldFailWhenEmailTemplateFails() throws Exception {
        RegistrationRequest mockRequest = createMockRequest();
        String mockRequestJson = mapToJson(mockRequest);

        doThrow(new EmailTemplateException("Failed to read email template")).when(authenticationService).register(any(RegistrationRequest.class));

        mockMvc.perform(post(REGISTER.getUrl()).contentType(CONTENT_TYPE).content(mockRequestJson)).andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
    }

    // authenticate method
    // -------------------------------------------------------------------------------------

    @Test
    void authenticateShouldReturnOkWithToken() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("tahiheb432@sigmazon.com", "password");
        AuthenticationResponse response = new AuthenticationResponse("jwt-token-123");

        when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(response);

        mockMvc.perform(post(AUTHENTICATION.getUrl())
                                .contentType(CONTENT_TYPE)
                                .accept(ACCEPT_TYPE)
                                .content(mapToJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(response.getToken()));

        verify(authenticationService, times(1)).authenticate(any(AuthenticationRequest.class));
    }


    @Test
    void authenticateShouldReturnUnauthorizedWithUserUnauthorizedException() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("tahiheb432@sigmazon.com",
                "password");
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


    private String mapToJson( Object request ) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    private RegistrationRequest createMockRequest() {
        return RegistrationRequest.builder().username("username").password("password").email("email@gmail.com").build();
    }

    @Test
    void authenticateShouldReturnBadRequestWithInvalidRequestFormat() throws Exception {
        String invalidRequestJson = "{\"email\": \"tahiheb432@sigmazon.com\"}";


        mockMvc.perform(post(AUTHENTICATION.getUrl())
                                .contentType(CONTENT_TYPE)
                                .accept(ACCEPT_TYPE)
                                .content(invalidRequestJson))
                .andExpect(status().isBadRequest());


        verify(authenticationService, never()).authenticate(any(AuthenticationRequest.class));
    }

    // activeAccount method
    // -------------------------------------------------------------------------------------

	@Test
	void activeAccountShouldReturnAccepted() throws Exception {
		String testActivationToken = "12345678";
		ResponseRecord response = new ResponseRecord(202, "test-token");

		when(authenticationService.activateAccount(testActivationToken)).thenReturn(response.message);

		mockMvc.perform(get(ACTIVE_ACCOUNT.getUrl())
				                .param("token", testActivationToken)
				                .accept(ACCEPT_TYPE)
				                .content(CONTENT_TYPE))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.code").value(response.code))
				.andExpect(jsonPath("$.message").value(response.message));

		verify(authenticationService, times(1)).activateAccount(testActivationToken);
	}

    @Test
    void activeAccountShouldReturnNotFoundWithTokenNotFoundException () throws Exception {
        String testActivationToken = "12345678";
        ErrorResponse response = new ErrorResponse(404, "Test Response Message");

        when(authenticationService.activateAccount(testActivationToken)).thenThrow(new TokenNotFoundException(response.message));

        mockMvc.perform(get(ACTIVE_ACCOUNT.getUrl())
                                .contentType(CONTENT_TYPE)
                                .accept(ACCEPT_TYPE)
                                .param("token", testActivationToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(authenticationService, times(1)).activateAccount(testActivationToken);
    }

    @Test
    void activeAccountShouldReturnBadRequestWithTokenExpiredException () throws Exception {

        String testActivationToken = "12345678";
        ErrorResponse response = new ErrorResponse(400, "Bad Request");

        when(authenticationService.activateAccount(testActivationToken)).thenThrow(new TokenExpiredException(response.message));

        mockMvc.perform(get(ACTIVE_ACCOUNT.getUrl())
                                .accept(ACCEPT_TYPE)
                                .contentType(CONTENT_TYPE)
                                .param("token", testActivationToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(authenticationService, times(1)).activateAccount(testActivationToken);
    }

    @Test
    void activeAccountShouldReturnNotFoundWithUserNotFoundException () throws Exception {
        String testActivationToken = "12345678";
        ErrorResponse response = new ErrorResponse(404, "Username not found");

        when(authenticationService.activateAccount(testActivationToken)).thenThrow(new UserNotFoundException(response.message));

        mockMvc.perform(get(ACTIVE_ACCOUNT.getUrl())
                                .accept(ACCEPT_TYPE)
                                .contentType(CONTENT_TYPE)
                                .param("token", testActivationToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(authenticationService, times(1)).activateAccount(testActivationToken);
    }

	@Test
	void activeAccountShouldReturnInternalServerError() throws Exception {
		String testActivationToken = "12345678";
		ErrorResponse response = new ErrorResponse(500, "Internal Server Error");

		when(authenticationService.activateAccount(testActivationToken)).thenThrow(new InternalError(response.message));

		mockMvc.perform(get(ACTIVE_ACCOUNT.getUrl())
				                .accept(ACCEPT_TYPE)
				                .contentType(CONTENT_TYPE)
				                .param("token", testActivationToken))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.errorCode").value(response.errorCode))
				.andExpect(jsonPath("$.message").value("Handler dispatch failed: java.lang" +
						                                       ".InternalError: " + response.message));

		verify(authenticationService, times(1)).activateAccount(testActivationToken);
	}
	// recoverByEmail method
	// -------------------------------------------------------------------------------------

    @Test
    void recoverByEmailShouldReturnOk() throws Exception {

        String email = "temp@mail.com";
        ResponseRecord response = new ResponseRecord(202, "Message have sent!");

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
    void recoverByEmailShouldReturnInternalErrorWithEmailTemplateException () throws Exception {
        String email = "temp@mail.com";

       ErrorResponse errorResponse = new ErrorResponse(500, "Error reading recovery email " +
                                                                     "template");

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

    // changePassword method
    // -------------------------------------------------------------------------------------

    @Test
    void changePasswordShouldReturnOk() throws Exception {
        ResponseRecord response = new ResponseRecord(200, "Password changed successfully");
        RecoveryRequest request = new RecoveryRequest("validToken", "newPassword");

        when(authenticationService.changeUserPassword(anyString(), anyString())).thenReturn(new com.dama.wanderwave.auth.ResponseRecord(200, "Password changed successfully"));


        mockMvc.perform(MockMvcRequestBuilders.post(CHANGE_PASSWORD.getUrl())
                                .accept(MediaType.APPLICATION_JSON)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(response.code))
                .andExpect(jsonPath("$.message").value(response.message));

        verify(authenticationService, times(1)).changeUserPassword(anyString(), anyString());
    }

    @Test
    void changePasswordShouldThrowTokenNotFoundException() throws Exception {
        ErrorResponse response = new ErrorResponse(404, "User not found");
        var mockRecoveryRequest = RecoveryRequest
                                          .builder()
                                          .password("password")
                                          .token("sigma-tkn")
                                          .build();

        doThrow(new TokenNotFoundException(response.message)).when(authenticationService).changeUserPassword(anyString(), anyString());


        mockMvc.perform(post(CHANGE_PASSWORD.getUrl())
                                .contentType(CONTENT_TYPE)
                                .accept(ACCEPT_TYPE)
                                .content(mapToJson(mockRecoveryRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(response.errorCode))
                .andExpect(jsonPath("$.message").value(response.message));;

        verify(authenticationService, times(1)).changeUserPassword(anyString(), anyString());

    }

    @Test
    void changePasswordShouldThrowWhenExceptionOccurs() throws Exception {
        var mockRecoveryRequest = RecoveryRequest
                                          .builder()
                                          .password("password")
                                          .token("sigma-tkn")
                                          .build();

        doThrow(new RuntimeException())
                .when(authenticationService)
                .changeUserPassword(anyString(), anyString());

        mockMvc.perform(post(CHANGE_PASSWORD.getUrl())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapToJson(mockRecoveryRequest)))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).changeUserPassword(anyString(), anyString());
    }


}


