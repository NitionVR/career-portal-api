package com.etalente.backend;

import com.etalente.backend.config.TestAwsConfig;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

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
public abstract class BaseIntegrationTest {
    // All test properties are now handled by the @TestPropertySource annotation
}
