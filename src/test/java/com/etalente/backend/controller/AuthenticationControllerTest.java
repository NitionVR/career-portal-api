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
    void exchangeOtt_shouldReturnJwt_whenOttIsValid() throws Exception {
        String ott = "valid-ott";
        String sessionJwt = "new-session-jwt";

        when(authenticationService.exchangeOneTimeTokenForJwt(ott)).thenReturn(sessionJwt);

        mockMvc.perform(post("/api/auth/exchange-ott")
                        .contentType(MediaType.TEXT_PLAIN) // OTT is sent as plain text in body
                        .content(ott))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(sessionJwt));
    }
}
