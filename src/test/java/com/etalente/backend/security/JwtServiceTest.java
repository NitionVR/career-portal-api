package com.etalente.backend.security;

import com.etalente.backend.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class JwtServiceTest extends BaseIntegrationTest {

    //SECRET=+q3uCqso90X1ZI7EfZJjfyDv9OeEfwoFmhvS3QTR

    @Autowired
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = new User("testuser@example.com", "password", new ArrayList<>());
    }

    @Test
    void testGenerateToken() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUsername() {
        String token = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);
        assertEquals("testuser@example.com", username);
    }

    @Test
    void testIsTokenValid() {
        String token = jwtService.generateToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testIsTokenInvalidWhenUsernameMismatch() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUserDetails = new User("otheruser@example.com", "password", new ArrayList<>());
        assertFalse(jwtService.isTokenValid(token, otherUserDetails));
    }
}
