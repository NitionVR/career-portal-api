package com.etalente.backend.service.impl;

import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @Test
    void initiateMagicLinkLogin_shouldCreateNewUser_whenUserNotFound() {
        // Given
        String email = "newuser@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("magic-token");

        // When
        authenticationService.initiateMagicLinkLogin(email);

        // Then
        verify(userRepository).save(any(User.class));
        verify(emailService).sendMagicLink(email, "http://localhost:4200/auth/callback?token=magic-token");
    }

    @Test
    void initiateMagicLinkLogin_shouldUseExistingUser_whenUserFound() {
        // Given
        String email = "existinguser@example.com";
        User existingUser = new User();
        existingUser.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("magic-token");

        // When
        authenticationService.initiateMagicLinkLogin(email);

        // Then
        verify(userRepository).findByEmail(email);
        verify(emailService).sendMagicLink(email, "http://localhost:4200/auth/callback?token=magic-token");
    }

    @Test
    void verifyMagicLinkAndIssueJwt_shouldReturnJwt_whenTokenIsValidAndUserExists() {
        // Given
        String token = "valid-token";
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(email, "", new java.util.ArrayList<>());

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(userDetails)).thenReturn("session-jwt");

        // When
        String jwt = authenticationService.verifyMagicLinkAndIssueJwt(token);

        // Then
        assertEquals("session-jwt", jwt);
    }

    @Test
    void verifyMagicLinkAndIssueJwt_shouldThrowException_whenTokenIsInvalid() {
        // Given
        String token = "invalid-token";
        String email = "test@example.com";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(email, "", new java.util.ArrayList<>());

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(false);

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authenticationService.verifyMagicLinkAndIssueJwt(token);
        });
    }

    @Test
    void verifyMagicLinkAndIssueJwt_shouldThrowException_whenUserNotFound() {
        // Given
        String token = "valid-token";
        String email = "test@example.com";
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(email, "", new java.util.ArrayList<>());

        when(jwtService.extractUsername(token)).thenReturn(email);
        when(jwtService.isTokenValid(token, userDetails)).thenReturn(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authenticationService.verifyMagicLinkAndIssueJwt(token);
        });
    }
}
