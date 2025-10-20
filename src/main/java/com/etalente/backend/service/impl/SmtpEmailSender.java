package com.etalente.backend.service.impl;

import com.etalente.backend.service.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Profile({"local", "dev", "smtp", "maildev", "google-smtp"})
public class SmtpEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender javaMailSender;
    private final String fromEmail;

    public SmtpEmailSender(JavaMailSender javaMailSender,
                          @Value("${spring.mail.from}") String fromEmail) {
        this.javaMailSender = javaMailSender;
        this.fromEmail = fromEmail;
        logger.info("========================================");
        logger.info("SmtpEmailSender initialized with:");
        logger.info("From Email: {}", fromEmail);
        logger.info("========================================");
    }

    @Override
    @org.springframework.scheduling.annotation.Async
    public void send(String to, String subject, String htmlBody) {
        logger.info("========================================");
        logger.info("Attempting to send email:");
        logger.info("To: {}", to);
        logger.info("Subject: {}", subject);
        logger.info("From: {}", fromEmail);
        logger.info("========================================");

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            logger.info("Sending email via JavaMailSender...");
            javaMailSender.send(message);
            logger.info("✅ SMTP Email sent successfully to {}", to);
            logger.info("========================================");
        } catch (Exception e) {
            logger.error("❌ Failed to send SMTP email to {}", to);
            logger.error("Error details:", e);
            logger.error("========================================");
            throw new RuntimeException("Failed to send SMTP email", e);
        }
    }
}
