package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

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
    private TestHelper testHelper;

    private final Faker faker = new Faker();

    @Test
    void updateProfile_shouldUpdateUser_whenAuthenticated() throws Exception {
        // Given
        String userEmail = faker.internet().emailAddress();
        String token = testHelper.createUserAndGetJwt(userEmail, Role.CANDIDATE);

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

        User updatedUser = userRepository.findByEmail(userEmail).get();
        assertEquals(firstName, updatedUser.getFirstName());
        assertEquals(lastName, updatedUser.getLastName());
        assertEquals(phone, updatedUser.getContactNumber());
        assertEquals(summary, updatedUser.getSummary());
    }
}