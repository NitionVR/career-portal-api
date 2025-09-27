package com.etalente.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    public void sendMagicLink(String to, String magicLink) {
        // For now, we will just log the magic link to the console.
        // In the future this would use an email sending library/service.
        logger.info("Sending magic link to {}: {}", to, magicLink);
    }
}
