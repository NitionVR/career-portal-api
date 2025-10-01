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
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private Faker faker;

    @BeforeEach
    void setUp() {
        faker = new Faker();
    }

    @Test
    void initiateRegistration_shouldSendEmail_forNewUser() throws Exception {
        // Given
        RegistrationRequest request = new RegistrationRequest(faker.internet().emailAddress(), Role.CANDIDATE);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration link sent to your email"));

        // Verify user not created yet
        assertFalse(userRepository.findByEmail(request.email()).isPresent());
    }

    @Test
    void initiateRegistration_shouldFail_forExistingUser() throws Exception {
        // Given
        User existingUser = createUser(Role.CANDIDATE);
        RegistrationRequest request = new RegistrationRequest(existingUser.getEmail(), Role.CANDIDATE);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void completeCandiateRegistration_shouldCreateUser() throws Exception {
        // Given
        String email = faker.internet().emailAddress();
        String token = generateRegistrationToken(email, Role.CANDIDATE);
        CandidateRegistrationDto dto = createFakeCandidateDto();

        // When & Then
        mockMvc.perform(post("/api/auth/register/candidate")
                        .param("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Verify user created
        User createdUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals(dto.username(), createdUser.getUsername());
        assertEquals(dto.firstName(), createdUser.getFirstName());
        assertEquals(dto.lastName(), createdUser.getLastName());
        assertEquals(Role.CANDIDATE, createdUser.getRole());
        assertTrue(createdUser.isProfileComplete());
        assertTrue(createdUser.isEmailVerified());
    }

    @Test
    void completeHiringManagerRegistration_shouldCreateUser() throws Exception {
        // Given
        String email = faker.internet().emailAddress();
        String token = generateRegistrationToken(email, Role.HIRING_MANAGER);
        HiringManagerRegistrationDto dto = createFakeHiringManagerDto();

        // When & Then
        mockMvc.perform(post("/api/auth/register/hiring-manager")
                        .param("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // Verify user created
        User createdUser = userRepository.findByEmail(email).orElseThrow();
        assertEquals(dto.username(), createdUser.getUsername());
        assertEquals(dto.companyName(), createdUser.getCompanyName());
        assertEquals(dto.industry(), createdUser.getIndustry());
        assertEquals(Role.HIRING_MANAGER, createdUser.getRole());
        assertTrue(createdUser.isProfileComplete());
        assertTrue(createdUser.isEmailVerified());
    }

    @Test
    void registration_shouldFail_withDuplicateUsername() throws Exception {
        // Given
        User existingUser = createUser(Role.CANDIDATE);
        String token = generateRegistrationToken(faker.internet().emailAddress(), Role.CANDIDATE);
        CandidateRegistrationDto dto = new CandidateRegistrationDto(
                existingUser.getUsername(), // Duplicate username
                faker.name().firstName(),
                faker.name().lastName(),
                "Male",
                "Asian",
                "None",
                faker.phoneNumber().cellPhone(),
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
        String token = generateRegistrationToken(faker.internet().emailAddress(), Role.HIRING_MANAGER);
        CandidateRegistrationDto dto = createFakeCandidateDto();

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
        String email = faker.internet().emailAddress();
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

    // Helper methods
    private User createUser(Role role) {
        User user = new User();
        user.setEmail(faker.internet().emailAddress());
        user.setUsername(faker.name().username());
        user.setRole(role);
        return userRepository.save(user);
    }

    private CandidateRegistrationDto createFakeCandidateDto() {
        return new CandidateRegistrationDto(
                faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                "Male",
                "Asian",
                "None",
                "+" + faker.number().digits(11),
                "+" + faker.number().digits(11)
        );
    }

    private HiringManagerRegistrationDto createFakeHiringManagerDto() {
        return new HiringManagerRegistrationDto(
                faker.name().username(),
                faker.company().name(),
                faker.company().industry(),
                faker.name().fullName(),
                "+" + faker.number().digits(11)
        );
    }

    private String generateRegistrationToken(String email, Role role) {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                email, "", new ArrayList<>());

        Map<String, Object> claims = new HashMap<>();
        claims.put("registration", true);
        claims.put("role", role.name());

        return jwtService.generateToken(claims, userDetails);
    }
}
