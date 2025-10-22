package com.etalente.backend.service.impl;

import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.model.RegistrationToken;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.model.Organization;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.RegistrationTokenRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.EmailService;
import com.etalente.backend.service.NovuNotificationService;
import com.etalente.backend.service.RegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final RegistrationTokenRepository registrationTokenRepository;
    private final NovuNotificationService novuNotificationService;

    @Value("${magic-link.url}")
    private String magicLinkUrl;

    @Value("${ott.expiration-minutes}")
    private long registrationTokenExpirationMinutes;

    public RegistrationServiceImpl(UserRepository userRepository,
                                   OrganizationRepository organizationRepository,
                                   EmailService emailService,
                                   JwtService jwtService,
                                   RegistrationTokenRepository registrationTokenRepository,
                                   NovuNotificationService novuNotificationService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.registrationTokenRepository = registrationTokenRepository;
        this.novuNotificationService = novuNotificationService;
    }

    @Override
    public void initiateRegistration(RegistrationRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BadRequestException("User with this email already exists");
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(registrationTokenExpirationMinutes);

        RegistrationToken registrationToken = new RegistrationToken(token, request.email(), request.role(), expiryDate);
        registrationTokenRepository.save(registrationToken);

        String magicLink = magicLinkUrl + "?token=" + token + "&action=register";
        emailService.sendRegistrationLink(request.email(), magicLink);
    }

    @Override
    public VerifyTokenResponse completeRegistration(String token, CandidateRegistrationDto dto) {
        RegistrationToken registrationToken = validateRegistrationToken(token);

        if (registrationToken.getRole() != Role.CANDIDATE) {
            throw new BadRequestException("Invalid registration token for candidate");
        }

        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new BadRequestException("Username already taken");
        }

        User user = new User();
        user.setEmail(registrationToken.getEmail());
        user.setUsername(dto.username());
        user.setRole(Role.CANDIDATE);
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setGender(dto.gender());
        user.setRace(dto.race());
        user.setDisability(dto.disability());
        user.setContactNumber(dto.contactNumber());
        user.setAlternateContactNumber(dto.alternateContactNumber());
        user.setEmailVerified(true);
        user.setProfileComplete(true);
        user.setNewUser(false); // Profile is complete now

        User savedUser = userRepository.save(user);
        novuNotificationService.sendWelcomeNotification(savedUser);

        return generateFirstLoginResponse(savedUser);
    }

    @Override
    public VerifyTokenResponse completeRegistration(String token, HiringManagerRegistrationDto dto) {
        RegistrationToken registrationToken = validateRegistrationToken(token);

        if (registrationToken.getRole() != Role.HIRING_MANAGER) {
            throw new BadRequestException("Invalid registration token for hiring manager");
        }

        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new BadRequestException("Username already taken");
        }

        User user = new User();
        user.setEmail(registrationToken.getEmail());
        user.setUsername(dto.username());
        user.setRole(Role.HIRING_MANAGER);
        user.setCompanyName(dto.companyName());
        user.setIndustry(dto.industry());
        user.setContactPerson(dto.contactPerson());
        user.setContactNumber(dto.contactNumber());
        user.setEmailVerified(true);
        user.setProfileComplete(true);
        user.setNewUser(false); // Profile is complete now

        User savedUser = userRepository.save(user);

        Organization organization = new Organization();
        organization.setName(dto.companyName());
        organization.setIndustry(dto.industry());
        organization.setCreatedBy(savedUser);
        organizationRepository.save(organization);

        savedUser.setOrganization(organization);
        User finalSavedUser = userRepository.save(savedUser);
        novuNotificationService.sendWelcomeNotification(finalSavedUser);

        return generateFirstLoginResponse(finalSavedUser);
    }

    private RegistrationToken validateRegistrationToken(String token) {
        RegistrationToken registrationToken = registrationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired registration token"));

        if (registrationToken.isUsed() || registrationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Invalid or expired registration token");
        }

        if (userRepository.findByEmail(registrationToken.getEmail()).isPresent()) {
            throw new BadRequestException("User already registered");
        }

        registrationToken.setUsed(true);
        return registrationTokenRepository.save(registrationToken);
    }

    @Override
    public Map<String, Object> validateToken(String token) {
        try {
            RegistrationToken registrationToken = registrationTokenRepository.findByToken(token)
                    .orElseThrow(() -> new BadRequestException("Token not found"));

            if (registrationToken.isUsed() || registrationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Token is invalid or expired");
            }

            return Map.of(
                    "email", registrationToken.getEmail(),
                    "role", registrationToken.getRole().name(),
                    "valid", true
            );
        } catch (Exception e) {
            return Map.of("valid", false, "error", e.getMessage());
        }
    }

    private VerifyTokenResponse generateFirstLoginResponse(User user) {
        String jwt = jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                false, // User is not new anymore
                user.getUsername()
        );

        VerifyTokenResponse.UserDto userDto = new VerifyTokenResponse.UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName(),
                false, // User is not new anymore
                null // profileImageUrl is null for new registrations
        );

        long expiresIn = jwtService.getTimeUntilExpiration(jwt);
        return new VerifyTokenResponse(jwt, userDto, expiresIn);
    }
}
