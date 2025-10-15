package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import org.json.JSONObject;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private JwtService jwtService;

    @Test
    void login_shouldReturnOk_whenEmailIsProvided() throws Exception {
        String email = "test@example.com";
        String requestBody = "{\"email\":\"" + email + "\"}";

        // Mock the service layer to do nothing when the magic link is initiated
        doNothing().when(authenticationService).initiateMagicLinkLogin(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string("Magic link sent. Please check your email."));
    }

    @Test
    void verify_shouldReturnJwt_whenTokenIsValid() throws Exception {
        String magicToken = "valid-magic-token";
        String sessionJwt = "new-session-jwt";

        // Mock the service layer to return a session JWT when the magic token is valid
        when(authenticationService.verifyMagicLinkAndIssueJwt(magicToken)).thenReturn(sessionJwt);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", magicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(sessionJwt));
    }

    @Test
    void verify_shouldReturnJwtWithClaims_whenTokenIsValid() throws Exception {
        String magicToken = "valid-magic-token";
        // Create a mock UserDetails for generating the JWT
        UserDetails userDetails = new org.springframework.security.core.userdetails.User("test@example.com", "", new ArrayList<>());

        // Prepare claims for the mock JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("is_new_user", true);
        claims.put("role", "CANDIDATE");

        // Generate a JWT with the desired claims
        String sessionJwtWithClaims = jwtService.generateToken(claims, userDetails);

        // Mock the service layer to return this JWT
        when(authenticationService.verifyMagicLinkAndIssueJwt(magicToken)).thenReturn(sessionJwtWithClaims);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", magicToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString());

        // Extract the token from the response and verify its claims
        String responseContent = mockMvc.perform(get("/api/auth/verify")
                        .param("token", magicToken))
                .andReturn().getResponse().getContentAsString();

        // Parse the JSON response to get the token string
        String tokenString = new JSONObject(responseContent).getString("token");

        // Extract claims from the token
        Claims extractedClaims = jwtService.extractAllClaims(tokenString);

        // Assert the claims
        assertThat(extractedClaims.get("is_new_user", Boolean.class)).isEqualTo(true);
        assertThat(extractedClaims.get("role", String.class)).isEqualTo("CANDIDATE");
    }
}
