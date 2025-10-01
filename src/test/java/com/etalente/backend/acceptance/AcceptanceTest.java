package com.etalente.backend.acceptance;

import com.intuit.karate.junit5.Karate;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("acceptance")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:15-alpine:///etalente",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "application.security.jwt.secret-key=NDg0MDRkNjM1MTY2NTQ2YTU3NmU1YTcyMzQ3NTM3NzgyMTQxMjU0NDJhNDcyZDRiNjE1MDY0NTM2NzU2NmI1OQ=="
})
class AcceptanceTest {

    @LocalServerPort
    int serverPort;

    @Karate.Test
    Karate runRegistration() {
        return Karate.run("registration")
                .systemProperty("local.server.port", "" + serverPort)
                .relativeTo(getClass());
    }
}
