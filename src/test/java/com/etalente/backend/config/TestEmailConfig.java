package com.etalente.backend.config;

import com.etalente.backend.service.EmailSender;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class TestEmailConfig {

    @Bean
    public EmailSender emailSender() {
        return Mockito.mock(EmailSender.class);
    }
}
