package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.AcceptInvitationRequest;
import com.etalente.backend.dto.RecruiterInvitationRequest;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.RecruiterInvitationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@Transactional
class InvitationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private RecruiterInvitationRepository invitationRepository;

    @Autowired
    private JwtService jwtService;

    private final Faker faker = new Faker();
    private com.etalente.backend.model.User hiringManager;
    private String hiringManagerJwt;

    @BeforeEach
    void setUp() {
        hiringManager = new com.etalente.backend.model.User();
        hiringManager.setEmail(faker.internet().emailAddress());
        hiringManager.setRole(Role.HIRING_MANAGER);
        hiringManager.setCompanyName(faker.company().name());
        hiringManager.setIndustry(faker.company().industry());
        userRepository.save(hiringManager);

        hiringManagerJwt = jwtService.generateToken(new org.springframework.security.core.userdetails.User(hiringManager.getEmail(), "", new ArrayList<>()));
    }

    @Test
    @DisplayName("POST /api/invitations/recruiter should return 201 CREATED when inviter is a hiring manager")
    void inviteRecruiter_shouldReturnCreated_whenUserIsHiringManager() throws Exception {
        // Given
        RecruiterInvitationRequest request = new RecruiterInvitationRequest(faker.internet().emailAddress(), "Welcome!");

        // When & Then
        mockMvc.perform(post("/api/invitations/recruiter")
                        .header("Authorization", "Bearer " + hiringManagerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(request.email()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertEquals(1, invitationRepository.count());
    }

    @Test
    @DisplayName("POST /api/invitations/accept/{token} should return 200 OK and create user")
    void acceptInvitation_shouldReturnOkAndJwt_whenTokenIsValid() throws Exception {
        // Given
        Organization org = new Organization();
        org.setName(hiringManager.getCompanyName());
        org.setCreatedBy(hiringManager);
        organizationRepository.save(org);

        RecruiterInvitation invitation = new RecruiterInvitation();
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setEmail(faker.internet().emailAddress());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(1));
        invitation.setOrganization(org);
        invitation.setInvitedBy(hiringManager);
        invitationRepository.save(invitation);

        AcceptInvitationRequest request = new AcceptInvitationRequest(
                faker.name().username(),
                faker.name().firstName(),
                faker.name().lastName(),
                "+1" + faker.number().digits(10),
                null
        );

        // When & Then
        mockMvc.perform(post("/api/invitations/accept/{token}", invitation.getToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.message").value("Invitation accepted successfully"));

        com.etalente.backend.model.User newUser = userRepository.findByEmail(invitation.getEmail()).orElseThrow();
        assertEquals(request.username(), newUser.getUsername());
        assertEquals(Role.RECRUITER, newUser.getRole());
        assertEquals(org.getId(), newUser.getOrganization().getId());
    }
}