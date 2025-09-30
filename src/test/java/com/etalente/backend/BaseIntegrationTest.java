package com.etalente.backend;

import com.etalente.backend.config.TestAwsConfig;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestAwsConfig.class})
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:tc:postgresql:15-alpine:///etalente");
        registry.add("application.security.jwt.secret-key",
                () -> "48404D635166546A576E5A7234753778214125442A472D4B6150645367566B59");
    }
}
