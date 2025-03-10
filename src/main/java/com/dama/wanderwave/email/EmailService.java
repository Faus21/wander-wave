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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static com.dama.wanderwave.email.TemplatePath.ACTIVATION;
import static com.dama.wanderwave.email.TemplatePath.RECOVERY;

@Getter
@RequiredArgsConstructor
enum TemplatePath {

    RECOVERY("templates/recovery-mail.html", "${resetToken}"),
    ACTIVATION("templates/activation-mail.html", "${activationToken}");

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
    public void sendRecoveryEmail(String emailToken, String to) throws IOException, MessagingException {
        log.info("Preparing to send recovery email to: {}", to);

        String htmlBody = prepareEmailBody(RECOVERY.getPath(), emailToken, RECOVERY.getPlaceHolder());
        sendEmail(to, htmlBody, recoveryTitle);

        log.info("Recovery email successfully sent to: {}", to);
    }

    @Async
    public void sendValidationEmail(String emailToken, String to) throws IOException, MessagingException {
        log.info("Preparing to send validation email to: {}", to);

        String htmlBody = prepareEmailBody(ACTIVATION.getPath(), emailToken, ACTIVATION.getPlaceHolder());
        sendEmail(to, htmlBody, validationTitle);

        log.info("Validation email successfully sent to: {}", to);
    }

    private String prepareEmailBody(String templatePath, String emailToken, String tokenPlaceholder) throws IOException {
        log.debug("Preparing email body using template: {}", templatePath);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(templatePath)) {
            if (inputStream == null) {
                throw new IOException("Template not found: " + templatePath);
            }
			String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			String preparedBody = template.replace(tokenPlaceholder, emailToken);

			log.debug("Email body prepared successfully");

			return preparedBody;
        }
    }

    private void sendEmail(String to, String htmlBody, String subject) throws MessagingException {
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
