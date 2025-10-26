package com.etalente.backend;

import com.etalente.backend.config.TestAwsConfig;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestAwsConfig.class})
@ComponentScan("com.etalente.backend")
@Tag("integration")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:15-alpine:///etalente",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "jwt.secret-key=NDg0MDRkNjM1MTY2NTQ2YTU3NmU1YTcyMzQ3NTM3NzgyMTQxMjU0NDJhNDcyZDRiNjE1MDY0NTM2NzU2NmI1OQ=="
})
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected EntityManager entityManager;

    protected void authenticateAs(UUID userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(), null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}