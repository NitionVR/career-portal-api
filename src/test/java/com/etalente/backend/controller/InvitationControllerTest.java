package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.AcceptInvitationRequest;
import com.etalente.backend.dto.RecruiterInvitationRequest;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.RecruiterInvitationRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private TestHelper testHelper;

    @Autowired
    private EntityManager entityManager;

    private final Faker faker = new Faker();

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();
    }

    @Test
    @DisplayName("POST /api/invitations/recruiter should return 201 CREATED when inviter is a hiring manager")
    void inviteRecruiter_shouldReturnCreated_whenUserIsHiringManager() throws Exception {
        // Given
        User hiringManager = testHelper.createUser(faker.internet().emailAddress(), Role.HIRING_MANAGER);

        String hiringManagerJwt = testHelper.generateJwtForUser(hiringManager);
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
    @DisplayName("POST /api/invitations/bulk-recruiter should return 200 OK when hiring manager invites multiple recruiters")
    void inviteRecruiterBulk_shouldReturnOk_whenHiringManagerInvitesMultipleRecruiters() throws Exception {
        // Given
        User hiringManager = testHelper.createUser(faker.internet().emailAddress(), Role.HIRING_MANAGER);

        String hiringManagerJwt = testHelper.generateJwtForUser(hiringManager);
        List<RecruiterInvitationRequest> requests = List.of(
                new RecruiterInvitationRequest(faker.internet().emailAddress(), "Welcome to the team!"),
                new RecruiterInvitationRequest(faker.internet().emailAddress(), "Join us!")
        );

        // When & Then
        mockMvc.perform(post("/api/invitations/bulk-recruiter")
                        .header("Authorization", "Bearer " + hiringManagerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Invitations processed"));

        assertEquals(2, invitationRepository.count());
    }

    @Test
    @DisplayName("POST /api/invitations/accept/{token} should return 200 OK and create user")
    void acceptInvitation_shouldReturnOkAndJwt_whenTokenIsValid() throws Exception {
        // Given
        User hiringManager = testHelper.createUser(faker.internet().emailAddress(), Role.HIRING_MANAGER);
        Organization org = hiringManager.getOrganization();

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

        User newUser = userRepository.findByEmail(invitation.getEmail()).orElseThrow();
        assertEquals(request.username(), newUser.getUsername());
        assertEquals(Role.RECRUITER, newUser.getRole());
        assertEquals(org.getId(), newUser.getOrganization().getId());
    }
}
