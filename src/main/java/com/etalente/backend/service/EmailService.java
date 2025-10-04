package com.etalente.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;

    public EmailService(EmailSender emailSender, TemplateEngine templateEngine) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
    }

    public void sendMagicLink(String to, String magicLink) {
        String subject = "Your Magic Link for Etalente";
        Context context = new Context();
        context.setVariable("magicLink", magicLink);
        String htmlBody = templateEngine.process("magic-link-email", context);
        emailSender.send(to, subject, htmlBody);
    }

    public void sendRegistrationLink(String to, String registrationLink) {
        String subject = "Complete Your Etalente Registration";
        Context context = new Context();
        context.setVariable("registrationLink", registrationLink);
        String htmlBody = templateEngine.process("registration-email", context);
        emailSender.send(to, subject, htmlBody);
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
        emailSender.send(to, subject, htmlBody);
    }
}