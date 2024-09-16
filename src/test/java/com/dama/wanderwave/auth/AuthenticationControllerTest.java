package com.dama.wanderwave.auth;


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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void registerNewUserShouldBeOk() throws Exception {
        RegistrationRequest mockRequest = RegistrationRequest.builder()
                .username("username")
                .password("password")
                .email("email@gmail.com")
                .build();

        String mockRequestJson = objectMapper.writeValueAsString(mockRequest);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mockRequestJson))
                .andExpect(status().isAccepted());

        verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
    }

    @Test
    void registerUserWithExistingUsernameShouldFail() throws Exception {
        RegistrationRequest mockRequest = RegistrationRequest.builder()
                .username("username")
                .password("password")
                .email("email@gmail.com")
                .build();

        String mockRequestJson = objectMapper.writeValueAsString(mockRequest);

        // Логика того, что юзер уже существует

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mockRequestJson))
                .andExpect(status().isInternalServerError());

        verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
    }

}
