package com.etalente.backend.service.impl;

import com.etalente.backend.model.OneTimeToken;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.EmailService;
import com.etalente.backend.repository.OneTimeTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("integration")
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private OneTimeTokenRepository oneTimeTokenRepository; // New

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "magicLinkUrl", "http://localhost:4200/auth/callback");
        ReflectionTestUtils.setField(authenticationService, "ottExpirationMinutes", 15L); // New
    }

    @Test
    void initiateMagicLinkLogin_shouldCreateNewUserWithDefaultRole_whenUserNotFound() {
        // Given
        String email = "newuser@example.com";
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setRole(Role.CANDIDATE);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser); // Return the actual new user

        // When
        authenticationService.initiateMagicLinkLogin(email);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(Role.CANDIDATE, userCaptor.getValue().getRole());

        ArgumentCaptor<OneTimeToken> ottCaptor = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(oneTimeTokenRepository).save(ottCaptor.capture());
        assertEquals(newUser, ottCaptor.getValue().getUser());
        assertFalse(ottCaptor.getValue().isUsed());
        assertTrue(ottCaptor.getValue().getExpiryDate().isAfter(LocalDateTime.now()));

        verify(emailService).sendMagicLink(eq(email), contains("http://localhost:4200/auth/callback?ott="));
    }

    @Test
    void initiateMagicLinkLogin_shouldUseExistingUser_whenUserFound() {
        // Given
        String email = "existinguser@example.com";
        User existingUser = new User();
        existingUser.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        // When
        authenticationService.initiateMagicLinkLogin(email);

        // Then
        verify(userRepository).findByEmail(email);

        ArgumentCaptor<OneTimeToken> ottCaptor = ArgumentCaptor.forClass(OneTimeToken.class);
        verify(oneTimeTokenRepository).save(ottCaptor.capture());
        assertEquals(existingUser, ottCaptor.getValue().getUser());
        assertFalse(ottCaptor.getValue().isUsed());
        assertTrue(ottCaptor.getValue().getExpiryDate().isAfter(LocalDateTime.now()));

        verify(emailService).sendMagicLink(eq(email), contains("http://localhost:4200/auth/callback?ott="));
    }

    @Test
    void exchangeOneTimeTokenForJwt_shouldReturnJwt_whenTokenIsValid() {
        // Given
        String ott = "valid-ott";
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.CANDIDATE);
        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, LocalDateTime.now().plusMinutes(10));

        when(oneTimeTokenRepository.findByToken(ott)).thenReturn(Optional.of(oneTimeToken));
        when(jwtService.generateToken(any(Map.class), any(UserDetails.class))).thenReturn("session-jwt");

        // When
        String jwt = authenticationService.exchangeOneTimeTokenForJwt(ott);

        // Then
        assertEquals("session-jwt", jwt);
        assertTrue(oneTimeToken.isUsed());
        verify(oneTimeTokenRepository).save(oneTimeToken);
    }

    @Test
    void exchangeOneTimeTokenForJwt_shouldThrowException_whenTokenIsInvalid() {
        // Given
        String ott = "invalid-ott";
        when(oneTimeTokenRepository.findByToken(ott)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authenticationService.exchangeOneTimeTokenForJwt(ott);
        });
    }

    @Test
    void exchangeOneTimeTokenForJwt_shouldThrowException_whenTokenIsUsed() {
        // Given
        String ott = "used-ott";
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.CANDIDATE);
        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, LocalDateTime.now().plusMinutes(10));
        oneTimeToken.setUsed(true);

        when(oneTimeTokenRepository.findByToken(ott)).thenReturn(Optional.of(oneTimeToken));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authenticationService.exchangeOneTimeTokenForJwt(ott);
        });
    }

    @Test
    void exchangeOneTimeTokenForJwt_shouldThrowException_whenTokenIsExpired() {
        // Given
        String ott = "expired-ott";
        User user = new User();
        user.setEmail("test@example.com");
        user.setRole(Role.CANDIDATE);
        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, LocalDateTime.now().minusMinutes(10));

        when(oneTimeTokenRepository.findByToken(ott)).thenReturn(Optional.of(oneTimeToken));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            authenticationService.exchangeOneTimeTokenForJwt(ott);
        });
    }
}
