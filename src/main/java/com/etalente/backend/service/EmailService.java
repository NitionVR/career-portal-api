package com.etalente.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final SesClient sesClient;
    private final String fromEmail;

    public EmailService(SesClient sesClient, @Value("${cloud.aws.ses.from}") String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    public void sendMagicLink(String to, String magicLink) {
        String subject = "Your Magic Link for Etalente";
        String htmlBody = """
            <html>
            <body>
                <h2>Welcome back to Etalente!</h2>
                <p>Click the link below to log in:</p>
                <p><a href="%s" style="background-color: #4CAF50; color: white; padding: 14px 20px; text-decoration: none; display: inline-block; border-radius: 4px;">Log In</a></p>
                <p>This link will expire in 15 minutes.</p>
                <p>If you didn't request this, please ignore this email.</p>
            </body>
            </html>
            """.formatted(magicLink);

        sendEmail(to, subject, htmlBody);
    }

    public void sendRegistrationLink(String to, String registrationLink) {
        String subject = "Complete Your Etalente Registration";
        String htmlBody = """
            <html>
            <body>
                <h2>Welcome to Etalente!</h2>
                <p>Thank you for registering. Click the link below to complete your registration:</p>
                <p><a href="%s" style="background-color: #2196F3; color: white; padding: 14px 20px; text-decoration: none; display: inline-block; border-radius: 4px;">Complete Registration</a></p>
                <p>This link will expire in 24 hours.</p>
                <p>If you didn't create an account, please ignore this email.</p>
            </body>
            </html>
            """.formatted(registrationLink);

        sendEmail(to, subject, htmlBody);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            Destination destination = Destination.builder().toAddresses(to).build();
            Content subjectContent = Content.builder().data(subject).build();
            Content htmlContent = Content.builder().data(htmlBody).build();
            Body body = Body.builder().html(htmlContent).build();
            Message message = Message.builder().subject(subjectContent).body(body).build();

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(destination)
                    .message(message)
                    .build();

            sesClient.sendEmail(request);
            logger.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}