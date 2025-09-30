package com.etalente.backend.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@Profile("test")
public class TestAwsConfig {

    @Bean
    @Primary
    public SesClient sesClient() {
        return Mockito.mock(SesClient.class);
    }
}
