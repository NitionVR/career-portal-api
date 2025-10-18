package com.etalente.backend.service.impl;

import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.RegistrationToken;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.RegistrationTokenRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.EmailService;
import com.etalente.backend.service.NovuNotificationService;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RegistrationTokenRepository registrationTokenRepository;
    @Mock
    private NovuNotificationService novuNotificationService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    private Faker faker;

    @BeforeEach
    void setUp() {
        faker = new Faker();
    }

    @Test
    void initiateRegistration_shouldSendEmail_forNewUser() {
        // Given
        RegistrationRequest request = new RegistrationRequest(faker.internet().emailAddress(), Role.CANDIDATE);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        // When
        registrationService.initiateRegistration(request);

        // Then
        verify(registrationTokenRepository).save(any(RegistrationToken.class));
        verify(emailService).sendRegistrationLink(eq(request.email()), anyString());
    }

    @Test
    void initiateRegistration_shouldThrowBadRequestException_forExistingUser() {
        // Given
        RegistrationRequest request = new RegistrationRequest(faker.internet().emailAddress(), Role.CANDIDATE);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

        // When & Then
        assertThrows(BadRequestException.class, () -> registrationService.initiateRegistration(request));
    }

    @Test
    void completeCandidateRegistration_shouldCreateUserAndReturnResponse_withValidToken() {
        // Given
        String token = UUID.randomUUID().toString();
        String email = faker.internet().emailAddress();
        CandidateRegistrationDto dto = new CandidateRegistrationDto(faker.name().username(), faker.name().firstName(), faker.name().lastName(), "Male", "Asian", "None", faker.phoneNumber().cellPhone(), null);
        RegistrationToken registrationToken = new RegistrationToken(token, email, Role.CANDIDATE, LocalDateTime.now().plusMinutes(15));

        when(registrationTokenRepository.findByToken(token)).thenReturn(Optional.of(registrationToken));
        when(registrationTokenRepository.save(any(RegistrationToken.class))).thenReturn(registrationToken); // Add this line
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.username())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID()); // Simulate DB generating an ID
            return user;
        });
        when(jwtService.generateToken(anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn("test-jwt");

        // When
        VerifyTokenResponse response = registrationService.completeRegistration(token, dto);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(email, savedUser.getEmail());
        assertEquals(dto.username(), savedUser.getUsername());
        assertEquals(Role.CANDIDATE, savedUser.getRole());
        assertTrue(savedUser.isProfileComplete());
        assertFalse(savedUser.isNewUser());
        assertNotNull(response);
        assertEquals("test-jwt", response.getToken());
        verify(novuNotificationService).sendWelcomeNotification(any(User.class));
    }

    @Test
    void completeHiringManagerRegistration_shouldCreateUserAndOrgAndReturnResponse_withValidToken() {
        // Given
        String token = UUID.randomUUID().toString();
        String email = faker.internet().emailAddress();
        HiringManagerRegistrationDto dto = new HiringManagerRegistrationDto(faker.name().username(), faker.company().name(), "IT", faker.name().fullName(), faker.phoneNumber().cellPhone());
        RegistrationToken registrationToken = new RegistrationToken(token, email, Role.HIRING_MANAGER, LocalDateTime.now().plusMinutes(15));

        when(registrationTokenRepository.findByToken(token)).thenReturn(Optional.of(registrationToken));
        when(registrationTokenRepository.save(any(RegistrationToken.class))).thenReturn(registrationToken); // Add this line
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(dto.username())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID()); // Simulate DB generating an ID
            return user;
        });
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(anyString(), anyString(), anyString(), anyBoolean(), anyString())).thenReturn("test-jwt");

        // When
        VerifyTokenResponse response = registrationService.completeRegistration(token, dto);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(email, savedUser.getEmail());
        assertEquals(Role.HIRING_MANAGER, savedUser.getRole());
        assertNotNull(savedUser.getOrganization());
        assertEquals(dto.companyName(), savedUser.getOrganization().getName());
        assertNotNull(response);
        assertEquals("test-jwt", response.getToken());
        verify(organizationRepository).save(any(Organization.class));
        verify(novuNotificationService).sendWelcomeNotification(any(User.class));
    }
}