package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class OrganizationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User hiringManager;
    private User recruiter;
    private Organization organization;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();

        hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);
        organization = hiringManager.getOrganization();

        recruiter = testHelper.createUser("rec@test.com", Role.RECRUITER);
        recruiter.setOrganization(organization);
        userRepository.save(recruiter);
    }

    protected void authenticateAsWithRoles(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getId().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/organization/members should return a list of organization members for Hiring Manager")
    void getOrganizationMembers_asHiringManager_shouldReturnMembers() throws Exception {
        authenticateAsWithRoles(hiringManager);

        mockMvc.perform(get("/api/organization/members")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.[0].email", is(hiringManager.getEmail())))
                .andExpect(jsonPath("$.[1].email", is(recruiter.getEmail())));
    }

    @Test
    @DisplayName("GET /api/organization/members should return a list of organization members for Recruiter")
    void getOrganizationMembers_asRecruiter_shouldReturnMembers() throws Exception {
        authenticateAsWithRoles(recruiter);

        mockMvc.perform(get("/api/organization/members")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.[0].email", is(hiringManager.getEmail())))
                .andExpect(jsonPath("$.[1].email", is(recruiter.getEmail())));
    }

    @Test
    @DisplayName("GET /api/organization/members should return 403 Forbidden for Candidate")
    void getOrganizationMembers_asCandidate_shouldReturnForbidden() throws Exception {
        User candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);
        authenticateAsWithRoles(candidate);

        mockMvc.perform(get("/api/organization/members")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/organization/members should return 403 Forbidden for unauthenticated user")
    void getOrganizationMembers_unauthenticated_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/organization/members")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
