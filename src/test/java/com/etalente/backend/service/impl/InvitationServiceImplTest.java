package com.etalente.backend.service.impl;

import com.etalente.backend.dto.AcceptInvitationRequest;
import com.etalente.backend.dto.RecruiterInvitationRequest;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.RecruiterInvitationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.service.EmailService;
import com.etalente.backend.service.TokenStore;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class InvitationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RecruiterInvitationRepository invitationRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private InvitationServiceImpl invitationService;

    @Mock
    private TokenStore tokenStore; // Mock the dependency

    private final Faker faker = new Faker();
    private User hiringManager;
    private Organization organization;

    @BeforeEach
    void setUp() {
        invitationService = new InvitationServiceImpl(userRepository, organizationRepository, invitationRepository, emailService, Optional.of(tokenStore));

        ReflectionTestUtils.setField(invitationService, "invitationLinkUrl", "http://localhost:4200/auth/accept-invitation");
        ReflectionTestUtils.setField(invitationService, "invitationExpiryHours", 72);

        hiringManager = new User();
        hiringManager.setId(UUID.randomUUID());
        hiringManager.setEmail(faker.internet().emailAddress());
        hiringManager.setFirstName(faker.name().firstName());
        hiringManager.setLastName(faker.name().lastName());
        hiringManager.setRole(Role.HIRING_MANAGER);

        organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName(faker.company().name());
        hiringManager.setOrganization(organization);
    }

    @Test
    @DisplayName("sendRecruiterInvitation should create and send invitation when inviter is a hiring manager")
    void sendRecruiterInvitation_shouldCreateAndSendInvitation_whenInviterIsHiringManager() {
        // Given
                hiringManager.setOrganization(null); // Start with no organization to test creation
                RecruiterInvitationRequest request = new RecruiterInvitationRequest(faker.internet().emailAddress(), "Join us!");
                when(userRepository.findById(hiringManager.getId())).thenReturn(Optional.of(hiringManager));
                when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> {
                    Organization org = invocation.getArgument(0);
                    org.setId(UUID.randomUUID()); // Simulate saving
                    return org;
                });
                when(userRepository.save(any(User.class))).thenReturn(hiringManager);
                when(invitationRepository.save(any(RecruiterInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
                // When
                invitationService.sendRecruiterInvitation(request, hiringManager.getId());
        
                // Then
                ArgumentCaptor<RecruiterInvitation> invitationCaptor = ArgumentCaptor.forClass(RecruiterInvitation.class);
                verify(invitationRepository).save(invitationCaptor.capture());
                RecruiterInvitation savedInvitation = invitationCaptor.getValue();
                ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
                verify(organizationRepository).save(organizationCaptor.capture());
                Organization savedOrganization = organizationCaptor.getValue();
        
                assertEquals(request.email(), savedInvitation.getEmail());
                assertEquals(savedOrganization.getId(), savedInvitation.getOrganization().getId()); // Assert on the created org
                assertEquals(hiringManager, savedInvitation.getInvitedBy());
                assertEquals(InvitationStatus.PENDING, savedInvitation.getStatus());
                assertNotNull(savedInvitation.getToken());
        
                verify(emailService).sendRecruiterInvitation(
                    eq(request.email()),
                    eq(hiringManager.getFirstName() + " " + hiringManager.getLastName()),
                    eq(savedOrganization.getName()),
                    anyString(),
                    eq(request.personalMessage())
                );
            }
        
            @Test
            @DisplayName("sendRecruiterInvitation should throw UnauthorizedException if inviter is not a hiring manager")
            void sendRecruiterInvitation_shouldThrowUnauthorizedException_whenInviterIsNotHiringManager() {
                // Given
                hiringManager.setRole(Role.CANDIDATE);
                RecruiterInvitationRequest request = new RecruiterInvitationRequest(faker.internet().emailAddress(), null);
                when(userRepository.findById(hiringManager.getId())).thenReturn(Optional.of(hiringManager));
        
                // When & Then
                assertThrows(UnauthorizedException.class, () -> {
                    invitationService.sendRecruiterInvitation(request, hiringManager.getId());
                });
            }

    @Test
    @DisplayName("acceptInvitation should create a new user with RECRUITER role")
    void acceptInvitation_shouldCreateNewUser_whenTokenIsValidAndUserDoesNotExist() {
        // Given
        RecruiterInvitation invitation = new RecruiterInvitation();
        invitation.setToken("valid-token");
        invitation.setEmail("new.recruiter@example.com");
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(1));
        invitation.setOrganization(organization);
        invitation.setInvitedBy(hiringManager);

        AcceptInvitationRequest request = new AcceptInvitationRequest(
            faker.name().username(),
            faker.name().firstName(),
            faker.name().lastName(),
            faker.phoneNumber().cellPhone(),
            null
        );

        when(invitationRepository.findByToken("valid-token")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByUsername(request.username())).thenReturn(false);
        when(userRepository.findByEmail(invitation.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User newUser = invitationService.acceptInvitation("valid-token", request);

        // Then
        assertEquals(request.username(), newUser.getUsername());
        assertEquals(invitation.getEmail(), newUser.getEmail());
        assertEquals(Role.RECRUITER, newUser.getRole());
        assertEquals(organization, newUser.getOrganization());
        assertTrue(newUser.isEmailVerified());
        assertTrue(newUser.isProfileComplete());
        assertEquals(InvitationStatus.ACCEPTED, invitation.getStatus());
    }

    @Test
    @DisplayName("acceptInvitation should throw BadRequestException for expired token")
    void acceptInvitation_shouldThrowBadRequestException_whenInvitationIsExpired() {
        // Given
        RecruiterInvitation invitation = new RecruiterInvitation();
        invitation.setToken("expired-token");
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().minusHours(1));

        AcceptInvitationRequest request = new AcceptInvitationRequest("user", "first", "last", "12345", null);

        when(invitationRepository.findByToken("expired-token")).thenReturn(Optional.of(invitation));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            invitationService.acceptInvitation("expired-token", request);
        });
        assertEquals(InvitationStatus.EXPIRED, invitation.getStatus());
    }
}
