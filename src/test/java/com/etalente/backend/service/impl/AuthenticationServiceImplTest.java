package com.etalente.backend.service.impl;

import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.OneTimeToken;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OneTimeTokenRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AuthenticationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private OneTimeTokenRepository oneTimeTokenRepository;

    @InjectMocks
    private AuthenticationServiceImpl authenticationService;

    private ArgumentCaptor<String> stringCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "magicLinkUrl", "http://localhost:4200/auth/callback");
        ReflectionTestUtils.setField(authenticationService, "ottExpirationMinutes", 15L);
        stringCaptor = ArgumentCaptor.forClass(String.class);
    }

    @Test
    void initiateMagicLinkLogin_shouldCreateNewUserWithDefaultRole_whenUserNotFound() {
        // Given
        String email = "newuser@example.com";
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setRole(Role.CANDIDATE);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

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

        verify(emailService).sendMagicLink(eq(email), anyString());
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
    }

    @Test
    void exchangeOneTimeTokenForJwt_shouldReturnResponse_whenTokenIsValid() {
        // Given
        String ott = "valid-ott";
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setRole(Role.CANDIDATE);
        user.setCreatedAt(LocalDateTime.now());
        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, LocalDateTime.now().plusMinutes(10));

        when(oneTimeTokenRepository.findByToken(ott)).thenReturn(Optional.of(oneTimeToken));
        when(jwtService.generateToken(any(), any(), any(), any(boolean.class), any())).thenReturn("session-jwt");
        when(jwtService.getTimeUntilExpiration("session-jwt")).thenReturn(3600000L);

        // When
        VerifyTokenResponse response = authenticationService.exchangeOneTimeTokenForJwt(ott);

        // Then
        assertNotNull(response);
        assertEquals("session-jwt", response.getToken());
        assertEquals(user.getId().toString(), response.getUser().getId());
        assertEquals(user.getEmail(), response.getUser().getEmail());
        assertTrue(oneTimeToken.isUsed());
        verify(oneTimeTokenRepository).save(oneTimeToken);
    }

    @Test
    void exchangeOneTimeTokenForJwt_shouldThrowException_whenTokenIsInvalid() {
        // Given
        String ott = "invalid-ott";
        when(oneTimeTokenRepository.findByToken(ott)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UnauthorizedException.class, () -> authenticationService.exchangeOneTimeTokenForJwt(ott));
    }

    @Test
    void exchangeOneTimeTokenForJwt_shouldThrowException_whenTokenIsUsed() {
        // Given
        String ott = "used-ott";
        User user = new User();
        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, LocalDateTime.now().plusMinutes(10));
        oneTimeToken.setUsed(true);

        when(oneTimeTokenRepository.findByToken(ott)).thenReturn(Optional.of(oneTimeToken));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> authenticationService.exchangeOneTimeTokenForJwt(ott));
    }

    @Test
    void exchangeOneTimeTokenForJwt_shouldThrowException_whenTokenIsExpired() {
        // Given
        String ott = "expired-ott";
        User user = new User();
        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, LocalDateTime.now().minusMinutes(10));

        when(oneTimeTokenRepository.findByToken(ott)).thenReturn(Optional.of(oneTimeToken));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> authenticationService.exchangeOneTimeTokenForJwt(ott));
    }
}