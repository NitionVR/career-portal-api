package com.etalente.backend.service.impl;

import com.etalente.backend.service.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
@Profile("ses") // Active when not in local or dev profile
public class SesEmailSender implements EmailSender {

    private static final Logger logger = LoggerFactory.getLogger(SesEmailSender.class);

    private final SesClient sesClient;
    private final String fromEmail;

    public SesEmailSender(SesClient sesClient, @Value("${cloud.aws.ses.from}") String fromEmail) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String to, String subject, String htmlBody) {
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
            logger.info("SES Email sent successfully to {}", to);
        } catch (Exception e) {
            logger.error("Failed to send SES email to {}", to, e);
            throw new RuntimeException("Failed to send SES email", e);
        }
    }
}
