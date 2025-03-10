package com.dama.wanderwave.email;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService Tests")
class EmailServiceTest {

    @Mock
    private EmailService emailService;

    @Nested
    @DisplayName("sendRecoveryEmail Method")
    class SendRecoveryEmailTests {

        @Test
        @DisplayName("Should throw IOException when recovery email fails")
        void sendRecoveryShouldFailWithWrongPath() throws Exception {
            doThrow(IOException.class).when(emailService).sendRecoveryEmail(anyString(), anyString());

            assertThrows(IOException.class,
                    () -> emailService.sendRecoveryEmail(anyString(), anyString()));
        }

        @Test
        @DisplayName("Should throw MessagingException when sending recovery email fails")
        void sendRecoveryShouldFailWithMessagingException() throws Exception {
            doThrow(MessagingException.class).when(emailService).sendRecoveryEmail(anyString(), anyString());

            assertThrows(MessagingException.class,
                    () -> emailService.sendRecoveryEmail(anyString(), anyString()));
        }

        @Test
        @DisplayName("Should send recovery email successfully")
        void sendRecoveryShouldBeOk() {
            assertDoesNotThrow(() -> emailService.sendRecoveryEmail(anyString(), anyString()));
        }
    }

    @Nested
    @DisplayName("sendValidationEmail Method")
    class SendValidationEmailTests {

        @Test
        @DisplayName("Should throw IOException when validation email fails")
        void sendValidationShouldFailWithWrongPath() throws Exception {
            doThrow(IOException.class).when(emailService).sendValidationEmail(anyString(), anyString());

            assertThrows(IOException.class,
                    () -> emailService.sendValidationEmail(anyString(), anyString()));
        }

        @Test
        @DisplayName("Should throw MessagingException when sending validation email fails")
        void sendValidationShouldFailWithMessagingException() throws Exception {
            doThrow(MessagingException.class).when(emailService).sendValidationEmail(anyString(), anyString());

            assertThrows(MessagingException.class,
                    () -> emailService.sendValidationEmail(anyString(), anyString()));
        }

        @Test
        @DisplayName("Should send validation email successfully")
        void sendValidationShouldBeOk() {
            assertDoesNotThrow(() -> emailService.sendValidationEmail(anyString(), anyString()));
        }
    }
}