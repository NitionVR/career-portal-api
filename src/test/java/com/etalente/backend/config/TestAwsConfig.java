package com.etalente.backend.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@Profile("test")
public class TestAwsConfig {

    @Bean
    @Primary
    public SesClient sesClient() {
        return Mockito.mock(SesClient.class);
    }

    @Bean
    @Primary
    public S3Client s3Client() {
        return Mockito.mock(S3Client.class);
    }

    @Bean
    @Primary
    public S3Presigner s3Presigner() {
        return Mockito.mock(S3Presigner.class);
    }
}
