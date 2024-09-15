package com.dama.wanderwave.email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class EmailServiceImpl implements IEmailService {

    @Value("${spring.mail.sender}")
    private String email_sender;

    private final JavaMailSender javaMailSender;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    @Async
    public void sendRecoveryEmail(String to) {
        try {
            String template = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/recovery-mail.html")));

            String resetLink = "test";
            String htmlBody = template.replace("${resetLink}", resetLink);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(email_sender);
            helper.setTo(to);
            helper.setSubject("WanderWave account recovery");
            helper.setText(htmlBody, true);

            javaMailSender.send(mimeMessage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
