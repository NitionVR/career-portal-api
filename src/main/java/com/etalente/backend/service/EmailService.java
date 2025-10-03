package com.etalente.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final SesClient sesClient;
    private final String fromEmail;
    private final TemplateEngine templateEngine;

    public EmailService(SesClient sesClient,
                        @Value("${cloud.aws.ses.from}") String fromEmail,
                        TemplateEngine templateEngine) {
        this.sesClient = sesClient;
        this.fromEmail = fromEmail;
        this.templateEngine = templateEngine;
    }

    public void sendMagicLink(String to, String magicLink) {
        String subject = "Your Magic Link for Etalente";
        Context context = new Context();
        context.setVariable("magicLink", magicLink);
        String htmlBody = templateEngine.process("magic-link-email", context);
        sendEmail(to, subject, htmlBody);
    }

    public void sendRegistrationLink(String to, String registrationLink) {
        String subject = "Complete Your Etalente Registration";
        Context context = new Context();
        context.setVariable("registrationLink", registrationLink);
        String htmlBody = templateEngine.process("registration-email", context);
        sendEmail(to, subject, htmlBody);
    }

    public void sendRecruiterInvitation(String to, String inviterName, String organizationName,
                                   String invitationLink, String personalMessage) {
        String subject = inviterName + " invited you to join " + organizationName + " as a Recruiter";
        Context context = new Context();
        context.setVariable("organizationName", organizationName);
        context.setVariable("inviterName", inviterName);
        context.setVariable("personalMessage", personalMessage);
        context.setVariable("invitationLink", invitationLink);
        String htmlBody = templateEngine.process("recruiter-invitation-email", context);
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
