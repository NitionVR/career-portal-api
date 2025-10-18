package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfileControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void updateProfile_shouldUpdateName() throws Exception {
        String token = testHelper.createUserAndGetJwt("test@example.com", Role.CANDIDATE);
        UUID userId = UUID.fromString(jwtService.extractUserId(token));

        Map<String, String> profileData = new HashMap<>();
        profileData.put("firstName", "John");
        profileData.put("lastName", "Doe");

        mockMvc.perform(put("/api/profile/update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileData)))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo("John");
        assertThat(updatedUser.getLastName()).isEqualTo("Doe");
    }

    @Test
    void completeProfile_shouldUpdateUserAndReturnNewToken() throws Exception {
        // 1. Create a new user who is marked as isNewUser=true
        User newUser = testHelper.createUser("newuser@example.com", Role.CANDIDATE);
        assertThat(newUser.isNewUser()).isTrue();
        String initialToken = testHelper.generateJwtForUser(newUser);

        // 2. Prepare the profile completion request
        CompleteProfileRequest request = new CompleteProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");

        // 3. Call the new endpoint
        mockMvc.perform(put("/api/profile/complete")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.isNewUser").value(false))
                .andExpect(jsonPath("$.user.firstName").value("Jane"));

        // 4. Verify the user is no longer marked as new in the database
        User updatedUser = userRepository.findById(newUser.getId()).orElseThrow();
        assertThat(updatedUser.isNewUser()).isFalse();
        assertThat(updatedUser.getFirstName()).isEqualTo("Jane");
    }
}
