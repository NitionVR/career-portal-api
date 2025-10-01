package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ProfileControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private Faker faker;

    @BeforeEach
    void setUp() {
        faker = new Faker();
    }

    @Test
    void updateProfile_shouldUpdateUser_whenAuthenticated() throws Exception {
        // Given
        User user = createUser(Role.CANDIDATE);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), "", Collections.emptyList());
        String token = jwtService.generateToken(userDetails);

        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        String phone = "+" + faker.number().digits(11);
        String summary = faker.lorem().sentence();

        String requestBody = String.format(
                "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"contactNumber\":\"%s\",\"summary\":\"%s\"}",
                firstName, lastName, phone, summary
        );

        // When & Then
        mockMvc.perform(put("/api/profile")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findByEmail(user.getEmail()).get();
        assertEquals(firstName, updatedUser.getFirstName());
        assertEquals(lastName, updatedUser.getLastName());
        assertEquals(phone, updatedUser.getContactNumber());
        assertEquals(summary, updatedUser.getSummary());
    }

    private User createUser(Role role) {
        User user = new User();
        user.setEmail(faker.internet().emailAddress());
        user.setRole(role);
        return userRepository.save(user);
    }
}
