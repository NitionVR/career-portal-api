package com.etalente.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Use ReflectionTestUtils to set the secret key and expiration for the test
        // This avoids needing a full Spring context
        String secret = "48404D635166546A576E5A7234753778214125442A472D4B6150645367566B59"; // A secure 64-byte key
        long expiration = 3600000; // 1 hour
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", expiration);

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

    @Test
    void testIsTokenExpired() {
        // Use a negative expiration to create an expired token
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String expiredToken = jwtService.generateToken(userDetails);
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> {
            jwtService.isTokenValid(expiredToken, userDetails);
        });
    }
}
