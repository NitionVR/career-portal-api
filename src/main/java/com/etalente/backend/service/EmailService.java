package com.etalente.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

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
        try {
            Destination destination = Destination.builder().toAddresses(to).build();
            Content subject = Content.builder().data("Your Magic Link for Etalente").build();
            Content textBody = Content.builder().data("Click the link to log in: " + magicLink).build();
            Body body = Body.builder().text(textBody).build();
            Message message = Message.builder().subject(subject).body(body).build();

            SendEmailRequest request = SendEmailRequest.builder()
                    .source(fromEmail)
                    .destination(destination)
                    .message(message)
                    .build();

            sesClient.sendEmail(request);
            logger.info("Magic link email sent to {}", to);
        } catch (Exception e) {
            // In a real application, you would have more robust error handling
            logger.error("Failed to send magic link email to {}", to, e);
        }
    }
}
