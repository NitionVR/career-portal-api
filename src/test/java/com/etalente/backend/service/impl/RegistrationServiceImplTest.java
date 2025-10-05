package com.etalente.backend.service.impl;

import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.model.Organization;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.EmailService;
import com.etalente.backend.service.TokenStore;
import com.github.javafaker.Faker;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class RegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private TokenStore tokenStore;

    private RegistrationServiceImpl registrationService;

    private Faker faker;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        // Manually inject mocks to handle Optional<TokenStore> correctly
        registrationService = new RegistrationServiceImpl(
                userRepository,
                organizationRepository,
                emailService,
                jwtService,
                Optional.of(tokenStore)
        );
    }

    @Test
    void initiateRegistration_shouldSendEmail_forNewUser() {
        // Given
        RegistrationRequest request = new RegistrationRequest(faker.internet().emailAddress(), Role.CANDIDATE);
        String fakeToken = faker.lorem().word();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(jwtService.generateToken(any(), any())).thenReturn(fakeToken);

        // When
        registrationService.initiateRegistration(request);

        // Then
        verify(emailService, times(1)).sendRegistrationLink(eq(request.email()), anyString());
        verify(tokenStore, times(1)).addToken(eq(request.email()), eq(fakeToken));
    }

    @Test
    void initiateRegistration_shouldThrowBadRequestException_forExistingUser() {
        // Given
        RegistrationRequest request = new RegistrationRequest(faker.internet().emailAddress(), Role.CANDIDATE);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(new User()));

        // When & Then
        assertThrows(BadRequestException.class, () -> registrationService.initiateRegistration(request));
        verify(emailService, never()).sendRegistrationLink(anyString(), anyString());
    }

    @Test
    void completeCandidateRegistration_shouldCreateUser_withValidToken() {
        // Given
        String email = faker.internet().emailAddress();
        String token = faker.lorem().word();
        CandidateRegistrationDto dto = createFakeCandidateDto();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(claims.get("role", String.class)).thenReturn(Role.CANDIDATE.name());
        when(claims.get("registration", Boolean.class)).thenReturn(true);

        when(jwtService.extractAllClaims(token)).thenReturn(claims);
        when(userRepository.findByUsername(dto.username())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registrationService.completeRegistration(token, dto);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(email, savedUser.getEmail());
        assertEquals(dto.username(), savedUser.getUsername());
        assertEquals(dto.firstName(), savedUser.getFirstName());
        assertEquals(dto.lastName(), savedUser.getLastName());
        assertEquals(Role.CANDIDATE, savedUser.getRole());
        assertTrue(savedUser.isProfileComplete());
        assertTrue(savedUser.isEmailVerified());
    }

    @Test
    void completeCandidateRegistration_shouldThrowBadRequest_whenUsernameExists() {
        // Given
        String token = faker.lorem().word();
        CandidateRegistrationDto dto = createFakeCandidateDto();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(faker.internet().emailAddress());
        when(claims.get("role", String.class)).thenReturn(Role.CANDIDATE.name());
        when(claims.get("registration", Boolean.class)).thenReturn(true);


        when(jwtService.extractAllClaims(token)).thenReturn(claims);
        when(userRepository.findByUsername(dto.username())).thenReturn(Optional.of(new User()));

        // When & Then
        assertThrows(BadRequestException.class, () -> registrationService.completeRegistration(token, dto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void completeCandidateRegistration_shouldThrowBadRequest_forWrongRoleToken() {
        // Given
        String token = faker.lorem().word();
        CandidateRegistrationDto dto = createFakeCandidateDto();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(faker.internet().emailAddress());
        when(claims.get("role", String.class)).thenReturn(Role.HIRING_MANAGER.name());
        when(claims.get("registration", Boolean.class)).thenReturn(true);

        when(jwtService.extractAllClaims(token)).thenReturn(claims);

        // When & Then
        assertThrows(BadRequestException.class, () -> registrationService.completeRegistration(token, dto));
    }

    @Test
    void completeHiringManagerRegistration_shouldCreateUser_withValidToken() {
        // Given
        String email = faker.internet().emailAddress();
        String token = faker.lorem().word();
        HiringManagerRegistrationDto dto = createFakeHiringManagerDto();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(claims.get("role", String.class)).thenReturn(Role.HIRING_MANAGER.name());
        when(claims.get("registration", Boolean.class)).thenReturn(true);

        when(jwtService.extractAllClaims(token)).thenReturn(claims);
        when(userRepository.findByUsername(dto.username())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registrationService.completeRegistration(token, dto);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(email, savedUser.getEmail());
        assertEquals(dto.username(), savedUser.getUsername());
        assertEquals(dto.companyName(), savedUser.getCompanyName());
        assertEquals(dto.industry(), savedUser.getIndustry());
        assertEquals(Role.HIRING_MANAGER, savedUser.getRole());
        assertTrue(savedUser.isProfileComplete());
        assertTrue(savedUser.isEmailVerified());
    }

    @Test
    void completeHiringManagerRegistration_shouldCreateOrganizationAndAssociateUser_withValidToken() {
        // Given
        String email = faker.internet().emailAddress();
        String token = faker.lorem().word();
        HiringManagerRegistrationDto dto = createFakeHiringManagerDto();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(claims.get("role", String.class)).thenReturn(Role.HIRING_MANAGER.name());
        when(claims.get("registration", Boolean.class)).thenReturn(true);

        when(jwtService.extractAllClaims(token)).thenReturn(claims);
        when(userRepository.findByUsername(dto.username())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getOrganization() == null) { // Simulate saving user before organization is set
                return user;
            }
            return user; // Simulate saving user after organization is set
        });
        when(organizationRepository.save(any(Organization.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        registrationService.completeRegistration(token, dto);

        // Then
        ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository, times(1)).save(organizationCaptor.capture());
        Organization savedOrganization = organizationCaptor.getValue();

        assertEquals(dto.companyName(), savedOrganization.getName());
        assertEquals(dto.industry(), savedOrganization.getIndustry());
        assertNotNull(savedOrganization.getCreatedBy());
        assertEquals(email, savedOrganization.getCreatedBy().getEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(userCaptor.capture()); // Called twice: once for initial user, once for user with organization
        User savedUser = userCaptor.getValue(); // Get the last saved user

        assertNotNull(savedUser.getOrganization());
        assertEquals(savedOrganization.getId(), savedUser.getOrganization().getId());
    }

    @Test
    void validateToken_shouldReturnValidInfo_forGoodToken() {
        // Given
        String token = faker.lorem().word();
        String email = faker.internet().emailAddress();
        Role role = Role.CANDIDATE;
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(claims.get("role", String.class)).thenReturn(role.name());
        when(claims.get("registration", Boolean.class)).thenReturn(true);

        when(jwtService.extractAllClaims(token)).thenReturn(claims);

        // When
        Map<String, Object> result = registrationService.validateToken(token);

        // Then
        assertTrue((Boolean) result.get("valid"));
        assertEquals(email, result.get("email"));
        assertEquals(role.name(), result.get("role"));
    }

    @Test
    void validateToken_shouldReturnInvalid_forBadToken() {
        // Given
        String token = "invalid-token";
        when(jwtService.extractAllClaims(token)).thenThrow(new RuntimeException("Invalid token"));

        // When
        Map<String, Object> result = registrationService.validateToken(token);

        // Then
        assertFalse((Boolean) result.get("valid"));
        assertNotNull(result.get("error"));
    }

    // Helper methods to create fake DTOs
    private CandidateRegistrationDto createFakeCandidateDto() {
        return new CandidateRegistrationDto(
                faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                "Male",
                "Asian",
                "None",
                faker.phoneNumber().cellPhone(),
                faker.phoneNumber().phoneNumber()
        );
    }

    private HiringManagerRegistrationDto createFakeHiringManagerDto() {
        return new HiringManagerRegistrationDto(
                faker.name().username(),
                faker.company().name(),
                faker.company().industry(),
                faker.name().fullName(),
                faker.phoneNumber().cellPhone()
        );
    }
}
