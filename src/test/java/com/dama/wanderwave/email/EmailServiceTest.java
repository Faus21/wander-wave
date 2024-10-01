package com.dama.wanderwave.email;

import jakarta.mail.MessagingException;
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
class EmailServiceTest {

    @Mock
    private EmailService emailService;

    @Test
    void sendRecoveryShouldFailWithWrongPath() throws Exception {
        doThrow(IOException.class).when(emailService).sendRecoveryEmail(anyString(), anyString());

        assertThrows(IOException.class,
                () -> emailService.sendRecoveryEmail(anyString(), anyString()));
    }

    @Test
    void sendRecoveryShouldFailWithMessagingException() throws Exception {
        doThrow(MessagingException.class).when(emailService).sendRecoveryEmail(anyString(), anyString());

        assertThrows(MessagingException.class,
                () -> emailService.sendRecoveryEmail(anyString(), anyString()));
    }

    @Test
    void sendValidationShouldFailWithWrongPath() throws Exception {
        doThrow(IOException.class).when(emailService).sendValidationEmail(anyString(), anyString());

        assertThrows(IOException.class,
                () -> emailService.sendValidationEmail(anyString(), anyString()));
    }

    @Test
    void sendValidationShouldFailWithMessagingException() throws Exception {
        doThrow(MessagingException.class).when(emailService).sendValidationEmail(anyString(), anyString());

        assertThrows(MessagingException.class,
                () -> emailService.sendValidationEmail(anyString(), anyString()));
    }

    @Test
    void sendRecoveryShouldBeOk() {
        assertDoesNotThrow(() -> emailService.sendRecoveryEmail(anyString(), anyString()));
    }

    @Test
    void sendValidationShouldBeOk() {
        assertDoesNotThrow(() -> emailService.sendValidationEmail(anyString(), anyString()));
    }


}
