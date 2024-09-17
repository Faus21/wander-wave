package com.dama.wanderwave.email;

import com.dama.wanderwave.handler.EmailSendingException;
import com.dama.wanderwave.handler.EmailTemplateException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

	private final JavaMailSender javaMailSender;
	@Value("${spring.mail.sender}")
	private String email_sender;
	@Value("${spring.mail.recovery-title}")
	private String recovery_title;
	@Value("${spring.mail.validation-title}")
	private String validation_title;

	@Async
	public void sendRecoveryEmail( String token, String to ) {
		try {
			String template = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/recovery-mail.html")));
			String htmlBody = template.replace("${resetToken}", token);
			sendEmail(to, htmlBody, recovery_title);
			log.info("Recovery email sent to {}", to);
		} catch ( IOException e ) {
			log.error("Error reading recovery email template: {}", e.getMessage());
			throw new EmailTemplateException("Error reading recovery email template");
		}
	}

	@Async
	public void sendValidationEmail( String token, String to ) {
		try {
			String template = new String(Files.readAllBytes(Paths.get("src/main/resources/templates/activation-mail.html")));
			String htmlBody = template.replace("${activationToken}", token);
			sendEmail(to, htmlBody, validation_title);
			log.info("Validation email sent to {}", to);
		} catch ( IOException e ) {
			log.error("Error reading validation email template: {}", e.getMessage());
			throw new EmailTemplateException("Error reading validation email template");
		}
	}

	private void sendEmail( String to, String htmlBody, String subject ) {
		try {
			MimeMessage mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			helper.setFrom(email_sender);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlBody, true);

			javaMailSender.send(mimeMessage);
		} catch ( MessagingException e ) {
			log.error("Error sending email: {}", e.getMessage());
			throw new EmailSendingException("Error sending email");
		}
	}
}