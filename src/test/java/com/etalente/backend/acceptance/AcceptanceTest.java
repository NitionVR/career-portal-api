package com.etalente.backend.acceptance;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.etalente.backend.config.TestTokenStore;
import org.springframework.context.annotation.Import;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("acceptance")
@Import(TestTokenStore.class) // Import the test-only bean
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:15-alpine:///etalente",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "application.security.jwt.secret-key=NDg0MDRkNjM1MTY2NTQ2YTU3NmU1YTcyMzQ3NTM3NzgyMTQxMjU0NDJhNDcyZDRiNjE1MDY0NTM2NzU2NmI1OQ==",
        "cloud.aws.ses.from=test@example.com"
})
class AcceptanceTest {

    @LocalServerPort
    int serverPort;

    @Karate.Test
    Karate runRegistration() {
        return Karate.run("registration")
                .karateEnv("test")  // Set karate environment
                .systemProperty("karate.server.port", String.valueOf(serverPort))
                .relativeTo(getClass());
    }

    @Karate.Test
    Karate runInvitation() {
        return Karate.run("invitation")
                .karateEnv("test")
                .systemProperty("karate.server.port", String.valueOf(serverPort))
                .relativeTo(getClass());
    }
}