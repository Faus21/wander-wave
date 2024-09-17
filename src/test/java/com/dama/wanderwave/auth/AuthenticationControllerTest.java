package com.dama.wanderwave.auth;


import com.dama.wanderwave.handler.GlobalExceptionHandler;
import com.dama.wanderwave.handler.TokenExpiredException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;
    private final String RECOVER_ACCOUNT_URL = "/api/auth/recover-account";
    private final String CHANGE_PASSWORD_URL = "/api/auth/change-password";

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private RecoveryRequest createMockRecoveryRequest() {
        return RecoveryRequest
                .builder()
                .password("password")
                .token("sigma-tkn")
                .build();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void recoverShouldReturnOkWhenAllIsOk() throws Exception {
        String email = "tahiheb432@sigmazon.com";
        doNothing().when(authenticationService).recoverAccount(email);

        mockMvc.perform(get(RECOVER_ACCOUNT_URL)
                        .param("email", email))
                .andExpect(status().isOk());

        verify(authenticationService, times(1)).recoverAccount(email);
    }

    @Test
    void recoverShouldReturn500WhenExceptionOccurs() throws Exception {
        String email = "error@sigmazon.com";

        doThrow(new RuntimeException()).when(authenticationService).recoverAccount(email);

        mockMvc.perform(get(RECOVER_ACCOUNT_URL)
                        .param("email", email))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).recoverAccount(email);
    }

    @Test
    void changePasswordShouldReturnOkWhenAllIsOk() throws Exception {
        var mockRecoveryRequest = createMockRecoveryRequest();

        doNothing()
                .when(authenticationService)
                .changeUserPassword(anyString(), anyString());

        mockMvc.perform(post(CHANGE_PASSWORD_URL)
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(mockRecoveryRequest)))
            .andExpect(status().isOk());

        verify(authenticationService, times(1)).changeUserPassword(anyString(), anyString());
    }

    @Test
    void changePasswordShouldReturn400WhenTokenIsExpired() throws Exception {
        var mockRecoveryRequest = createMockRecoveryRequest();

        doThrow(new TokenExpiredException(""))
                .when(authenticationService)
                .changeUserPassword(anyString(), anyString());

        mockMvc.perform(post(CHANGE_PASSWORD_URL)
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(mockRecoveryRequest)))
            .andExpect(status().isBadRequest());

        verify(authenticationService, times(1)).changeUserPassword(anyString(), anyString());
    }

    @Test
    void changePasswordShouldThrow500WhenExceptionOccurs() throws Exception {
        var mockRecoveryRequest = createMockRecoveryRequest();

        doThrow(new RuntimeException())
                .when(authenticationService)
                .changeUserPassword(anyString(), anyString());

        mockMvc.perform(post(CHANGE_PASSWORD_URL)
                 .contentType(MediaType.APPLICATION_JSON)
                 .content(objectMapper.writeValueAsString(mockRecoveryRequest)))
            .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).changeUserPassword(anyString(), anyString());
    }
}

