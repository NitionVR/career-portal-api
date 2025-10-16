package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.VerifyTokenResponse;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

        doNothing().when(authenticationService).initiateMagicLinkLogin(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Magic link sent to your email"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void verifyToken_shouldReturnVerifyTokenResponse_whenTokenIsValid() throws Exception {
        String ott = "valid-ott";
        String jwt = "new-session-jwt";
        String userId = "user-id";
        String email = "test@example.com";
        String role = "CANDIDATE";

        VerifyTokenResponse.UserDto userDto = new VerifyTokenResponse.UserDto(userId, email, role, "Test", "User", false);
        VerifyTokenResponse response = new VerifyTokenResponse(jwt, userDto, 3600000L);

        when(authenticationService.exchangeOneTimeTokenForJwt(ott)).thenReturn(response);

        mockMvc.perform(get("/api/auth/verify")
                        .param("token", ott))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(jwt))
                .andExpect(jsonPath("$.user.id").value(userId))
                .andExpect(jsonPath("$.user.email").value(email));
    }

    @Test
    void checkSession_shouldReturnAuthenticatedResponse_whenTokenIsValid() throws Exception {
        String userId = "user-id";
        String email = "test@example.com";
        String role = "CANDIDATE";
        String jwt = jwtService.generateToken(userId, email, role, false, null);

        mockMvc.perform(get("/api/auth/session")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value(role));
    }

    @Test
    void checkSession_shouldReturnUnauthenticatedResponse_whenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    void refreshToken_shouldReturnNewToken() throws Exception {
        String userId = "user-id";
        String email = "test@example.com";
        String role = "CANDIDATE";
        String jwt = jwtService.generateToken(userId, email, role, false, "testuser");

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void logout_shouldReturnSuccessMessage() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}