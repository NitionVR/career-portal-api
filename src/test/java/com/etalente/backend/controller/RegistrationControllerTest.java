package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Transactional
class RegistrationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void initiateRegistration_shouldSendEmail_forNewUser() throws Exception {
        // Given
        RegistrationRequest request = new RegistrationRequest(
            "newuser@example.com",
            Role.CANDIDATE
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration link sent to your email"));

        // Verify user not created yet
        assertFalse(userRepository.findByEmail("newuser@example.com").isPresent());
    }

    @Test
    void initiateRegistration_shouldFail_forExistingUser() throws Exception {
        // Given
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setRole(Role.CANDIDATE);
        userRepository.save(existingUser);

        RegistrationRequest request = new RegistrationRequest(
            "existing@example.com",
            Role.CANDIDATE
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeCandiateRegistration_shouldCreateUser() throws Exception {
        // Given
        String email = "candidate@example.com";
        String token = generateRegistrationToken(email, Role.CANDIDATE);

        CandidateRegistrationDto dto = new CandidateRegistrationDto(
            "candidate123",
            "John",
            "Doe",
            "Male",
            "Asian",
            "None",
            "+1234567890",
            "+1987654321"
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register/candidate")
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Verify user created
        User createdUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals("candidate123", createdUser.getUsername());
        assertEquals("John", createdUser.getFirstName());
        assertEquals("Doe", createdUser.getLastName());
        assertEquals(Role.CANDIDATE, createdUser.getRole());
        assertTrue(createdUser.isProfileComplete());
        assertTrue(createdUser.isEmailVerified());
    }

    @Test
    void completeHiringManagerRegistration_shouldCreateUser() throws Exception {
        // Given
        String email = "hiring@example.com";
        String token = generateRegistrationToken(email, Role.HIRING_MANAGER);

        HiringManagerRegistrationDto dto = new HiringManagerRegistrationDto(
            "hiringmgr123",
            "Tech Corp",
            "Information Technology",
            "Jane Smith",
            "+1234567890"
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register/hiring-manager")
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Verify user created
        User createdUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals("hiringmgr123", createdUser.getUsername());
        assertEquals("Tech Corp", createdUser.getCompanyName());
        assertEquals("Information Technology", createdUser.getIndustry());
        assertEquals(Role.HIRING_MANAGER, createdUser.getRole());
        assertTrue(createdUser.isProfileComplete());
        assertTrue(createdUser.isEmailVerified());
    }

    @Test
    void registration_shouldFail_withDuplicateUsername() throws Exception {
        // Given
        User existingUser = new User();
        existingUser.setEmail("existing@example.com");
        existingUser.setUsername("takenusername");
        existingUser.setRole(Role.CANDIDATE);
        userRepository.save(existingUser);

        String token = generateRegistrationToken("newuser@example.com", Role.CANDIDATE);

        CandidateRegistrationDto dto = new CandidateRegistrationDto(
            "takenusername", // Duplicate username
            "John",
            "Doe",
            "Male",
            "Asian",
            "None",
            "+1234567890",
            null
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register/candidate")
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registration_shouldFail_withWrongRoleToken() throws Exception {
        // Given
        String token = generateRegistrationToken("user@example.com", Role.HIRING_MANAGER);

        CandidateRegistrationDto dto = new CandidateRegistrationDto(
            "username",
            "John",
            "Doe",
            null,
            null,
            null,
            "+1234567890",
            null
        );

        // When & Then
        mockMvc.perform(post("/api/auth/register/candidate")
                .param("token", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid registration token for candidate"));
    }

    @Test
    void validateRegistrationToken_shouldReturnTokenInfo() throws Exception {
        // Given
        String email = "test@example.com";
        Role role = Role.CANDIDATE;
        String token = generateRegistrationToken(email, role);

        // When & Then
        mockMvc.perform(get("/api/auth/validate-registration-token")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validateRegistrationToken_shouldReturnInvalid_forBadToken() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/validate-registration-token")
                .param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.error").exists());
    }

    // Helper method to generate registration tokens
    private String generateRegistrationToken(String email, Role role) {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "", new ArrayList<>());

        Map<String, Object> claims = new HashMap<>();
        claims.put("registration", true);
        claims.put("role", role.name());

        return jwtService.generateToken(claims, userDetails);
    }
}
