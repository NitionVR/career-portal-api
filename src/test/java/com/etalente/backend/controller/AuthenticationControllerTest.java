package com.etalente.backend.controller;

import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;


    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

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
                .andExpect(content().string(sessionJwt));
    }
}
