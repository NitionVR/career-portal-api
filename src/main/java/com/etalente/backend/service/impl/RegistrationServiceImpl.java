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
import com.etalente.backend.service.RegistrationService;
import com.etalente.backend.service.TokenStore;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationServiceImpl.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final Optional<TokenStore> tokenStore;


    @Value("${magic-link-url}")
    private String magicLinkUrl;

    public RegistrationServiceImpl(UserRepository userRepository,
                                  OrganizationRepository organizationRepository,
                                  EmailService emailService,
                                  JwtService jwtService,
                                  @Autowired(required = false) Optional<TokenStore> tokenStore) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
    }

    @Override
    public void initiateRegistration(RegistrationRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BadRequestException("User with this email already exists");
        }

        // Generate registration token with role
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                request.email(), "", new ArrayList<>());

        Map<String, Object> claims = new HashMap<>();
        claims.put("registration", true);
        claims.put("role", request.role().name());
        String token = jwtService.generateToken(claims, userDetails);

        // Store token for test purposes if the store is present
        tokenStore.ifPresent(store -> {
            logger.info("Storing registration token for email: {}", request.email());
            store.addToken(request.email(), token);
        });

        // Send magic link
        String magicLink = magicLinkUrl + "?token=" + token + "&action=register";
        emailService.sendRegistrationLink(request.email(), magicLink);
    }

    @Override
    public User completeRegistration(String token, CandidateRegistrationDto dto) {
        Claims claims = validateRegistrationToken(token);
        String email = claims.getSubject();
        String roleStr = claims.get("role", String.class);

        if (!Role.CANDIDATE.name().equals(roleStr)) {
            throw new BadRequestException("Invalid registration token for candidate");
        }

        // Check username uniqueness
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new BadRequestException("Username already taken");
        }

        User user = new User();
        user.setEmail(email);
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

        return userRepository.save(user);
    }

    @Override
    public User completeRegistration(String token, HiringManagerRegistrationDto dto) {
        Claims claims = validateRegistrationToken(token);
        String email = claims.getSubject();
        String roleStr = claims.get("role", String.class);

        if (!Role.HIRING_MANAGER.name().equals(roleStr)) {
            throw new BadRequestException("Invalid registration token for hiring manager");
        }

        // Check username uniqueness
        if (userRepository.findByUsername(dto.username()).isPresent()) {
            throw new BadRequestException("Username already taken");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(dto.username());
        user.setRole(Role.HIRING_MANAGER);
        user.setCompanyName(dto.companyName());
        user.setIndustry(dto.industry());
        user.setContactPerson(dto.contactPerson());
        user.setContactNumber(dto.contactNumber());
        user.setEmailVerified(true);
        user.setProfileComplete(true);

        User savedUser = userRepository.save(user); // Save user first to get an ID

        // Create Organization for Hiring Manager
        Organization organization = new Organization();
        organization.setName(dto.companyName());
        organization.setIndustry(dto.industry());
        organization.setCreatedBy(savedUser); // Use the saved user as the creator
        organizationRepository.save(organization);

        savedUser.setOrganization(organization); // Associate user with the new organization

        return userRepository.save(savedUser); // Save user again to update the organization
    }

    private Claims validateRegistrationToken(String token) {
        try {
            Claims claims = jwtService.extractAllClaims(token);

            // Verify it's a registration token
            Boolean isRegistration = claims.get("registration", Boolean.class);
            if (!Boolean.TRUE.equals(isRegistration)) {
                throw new BadRequestException("Invalid registration token");
            }

            // Check if user already exists
            String email = claims.getSubject();
            if (userRepository.findByEmail(email).isPresent()) {
                throw new BadRequestException("User already registered");
            }

            return claims;
        } catch (Exception e) {
            throw new BadRequestException("Invalid or expired registration token");
        }
    }

    @Override
    public Map<String, Object> validateToken(String token) {
        try {
            Claims claims = jwtService.extractAllClaims(token);

            Boolean isRegistration = claims.get("registration", Boolean.class);
            if (!Boolean.TRUE.equals(isRegistration)) {
                throw new BadRequestException("Invalid registration token");
            }

            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            return Map.of(
                "email", email,
                "role", role,
                "valid", true
            );
        } catch (Exception e) {
            return Map.of("valid", false, "error", e.getMessage());
        }
    }
}
