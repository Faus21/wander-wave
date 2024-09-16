package com.dama.wanderwave.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.sender}")
    private String email_sender;
    @Value("${spring.mail.recovery-title}")
    private String recovery_title;
    @Value("${spring.mail.validation-title}")
    private String validation_title;

    private final JavaMailSender javaMailSender;

    @Async
    public void sendRecoveryEmail(String to) {
        try {
            String template = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/recovery-mail.html")));
            String resetLink = "test";
            String htmlBody = template.replace("${resetToken}", resetLink);
            sendEmail(to, htmlBody, recovery_title);
            log.info("Recovery email sent to {}", to);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Async
    public void sendValidationEmail(String token, String to) {
        try {
            String template = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/activation-mail.html")));
            String htmlBody = template.replace("${activationToken}", token);
            sendEmail(to, htmlBody, validation_title);
            log.info("Validation email sent to {}", to);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void sendEmail(String to, String htmlBody, String subject) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(email_sender);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);

        javaMailSender.send(mimeMessage);
    }
}
