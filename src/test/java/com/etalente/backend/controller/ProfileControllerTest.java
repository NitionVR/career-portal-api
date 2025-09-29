package com.etalente.backend.controller;

import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
class ProfileControllerTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("application.security.jwt.secret-key", () -> "48404D635166546A576E5A7234753778214125442A472D4B6150645367566B59");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void updateProfile_shouldUpdateUser_whenAuthenticated() throws Exception {
        // Given
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.CANDIDATE);
        userRepository.save(user);

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), "", new java.util.ArrayList<>());
        String token = jwtService.generateToken(userDetails);

        String requestBody = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"phone\":\"1234567890\",\"summary\":\"A summary\"}";

        // When & Then
        mockMvc.perform(put("/api/profile")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findByEmail("test@example.com").get();
        assertEquals("John", updatedUser.getFirstName());
        assertEquals("Doe", updatedUser.getLastName());
    }
}
