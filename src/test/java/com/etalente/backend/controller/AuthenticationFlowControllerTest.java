package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.model.OneTimeToken;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OneTimeTokenRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthenticationFlowControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OneTimeTokenRepository oneTimeTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_shouldReturnNotFound_whenEmailDoesNotExist() throws Exception {
        String nonExistentEmail = "nonexistent-" + UUID.randomUUID() + "@test.com";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + nonExistentEmail + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with email " + nonExistentEmail + " not found."));
    }

    @Test
    void login_shouldReturnOk_whenEmailExists() throws Exception {
        User user = testHelper.createUser("existing-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + user.getEmail() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Magic link sent to your email"));
    }

    void verify_shouldReturnCorrectPayload_forNewUser() throws Exception {
        // User is created with isNewUser = true by default
        User user = testHelper.createUser("verify-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);

        String ott = UUID.randomUUID().toString();
        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, LocalDateTime.now().plusMinutes(15));
        oneTimeTokenRepository.save(oneTimeToken);

        MvcResult result = mockMvc.perform(get("/api/auth/verify").param("token", ott))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.user.email").value(user.getEmail()))
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.user.isNewUser").value(true)) // Expect true on first login
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn();

        // Verify the isNewUser flag was updated to false in the database
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.isNewUser()).isFalse();
    }

    @Test
    void session_shouldReturnAuthenticated_whenTokenIsValid() throws Exception {
        User user = testHelper.createUser("session-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
        user.setFirstName("Test");
        user.setLastName("User");
        userRepository.save(user);
        String token = testHelper.generateJwtForUser(user);

        mockMvc.perform(get("/api/auth/session")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value(user.getEmail()))
                .andExpect(jsonPath("$.user.role").value("RECRUITER"))
                .andExpect(jsonPath("$.user.firstName").value("Test"));
    }

    @Test
    void session_shouldReturnUnauthenticated_whenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void refresh_shouldReturnNewToken_whenTokenIsValid() throws Exception {
        String oldToken = testHelper.createUserAndGetJwt("refresh-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);

        // Add a delay to ensure the new token's timestamp is different
        Thread.sleep(1000);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
        String newToken = (String) responseMap.get("token");

        assertThat(newToken).isNotNull();
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(jwtService.extractEmail(newToken)).isEqualTo(jwtService.extractEmail(oldToken));
    }

    @Test
    void logout_shouldReturnSuccessMessage() throws Exception {
        String token = testHelper.createUserAndGetJwt("logout-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
