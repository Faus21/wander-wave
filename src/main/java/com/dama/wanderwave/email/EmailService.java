package com.dama.wanderwave.email;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.dama.wanderwave.email.TemplatePath.ACTIVATION;
import static com.dama.wanderwave.email.TemplatePath.RECOVERY;
import static java.nio.file.Paths.*;

@Getter
@RequiredArgsConstructor
enum TemplatePath {

    RECOVERY("src/main/resources/templates/recovery-mail.html", "${resetToken}"), ACTIVATION("src/main/resources/templates/activation-mail.html", "${activationToken}");

    private final String path;
    private final String placeHolder;
}

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    @Value("${spring.mail.sender}")
    private String emailSender;
    @Value("${spring.mail.recovery-title}")
    private String recoveryTitle;
    @Value("${spring.mail.validation-title}")
    private String validationTitle;

    @Async
    public void sendRecoveryEmail( String token, String to ) throws IOException, MessagingException {
        log.info("Preparing to send recovery email to: {}", to);

        String htmlBody = prepareEmailBody(RECOVERY.getPath(), token, RECOVERY.getPlaceHolder());
        sendEmail(to, htmlBody, recoveryTitle);

        log.info("Recovery email successfully sent to: {}", to);
    }

    @Async
    public void sendValidationEmail( String token, String to ) throws IOException, MessagingException {
        log.info("Preparing to send validation email to: {}", to);

        String htmlBody = prepareEmailBody(ACTIVATION.getPath(), token, ACTIVATION.getPlaceHolder());
        sendEmail(to, htmlBody, validationTitle);

        log.info("Validation email successfully sent to: {}", to);

    }

    private String prepareEmailBody( String templatePath, String token, String tokenPlaceholder ) throws IOException {
        log.debug("Preparing email body using template: {}", templatePath);

        String template = new String(Files.readAllBytes(get(templatePath)));
        String preparedBody = template.replace(tokenPlaceholder, token);

        log.debug("Email body prepared successfully");

        return preparedBody;

    }

    private void sendEmail( String to, String htmlBody, String subject ) throws MessagingException {
        log.debug("Preparing to send email. To: {}, Subject: {}", to, subject);
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(emailSender);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        javaMailSender.send(mimeMessage);

        log.debug("Email sent successfully. To: {}, Subject: {}", to, subject);

    }
}
