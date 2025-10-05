package com.etalente.backend;

import com.etalente.backend.config.TestAwsConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({TestAwsConfig.class})
@Tag("integration")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:15-alpine:///etalente",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "jwt.secret-key=NDg0MDRkNjM1MTY2NTQ2YTU3NmU1YTcyMzQ3NTM3NzgyMTQxMjU0NDJhNDcyZDRiNjE1MDY0NTM2NzU2NmI1OQ=="
})
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearDatabase() {
        entityManager.createNativeQuery("TRUNCATE TABLE users, organizations, recruiter_invitations, job_posts RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }
}