package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.ApplicationTransitionRequest;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobApplicationRepository;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JobApplicationTransitionControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    private User hiringManager;
    private User recruiter;
    private User candidate;
    private Organization organization;
    private JobPost jobPost;
    private JobApplication jobApplication;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();

        // Create organization and users
        hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);
        organization = hiringManager.getOrganization();

        recruiter = testHelper.createUser("recruiter@test.com", Role.RECRUITER);
        recruiter.setOrganization(organization);
        recruiter = userRepository.save(recruiter);

        candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);

        // Create job post
        authenticateAs(hiringManager.getId(), Role.HIRING_MANAGER);
        jobPost = new JobPost();
        jobPost.setTitle("Test Job");
        jobPost.setCompany("Test Company");
        jobPost.setJobType("Full-time");
        jobPost.setDescription("Test Description");
        jobPost.setRemote("Remote");
        jobPost.setExperienceLevel("Senior");
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPost.setCreatedBy(hiringManager);
        jobPost.setOrganization(organization);
        jobPost = jobPostRepository.save(jobPost);

        // Create job application
        jobApplication = new JobApplication();
        jobApplication.setCandidate(candidate);
        jobApplication.setJobPost(jobPost);
        jobApplication.setStatus(JobApplicationStatus.APPLIED);
        jobApplication.setApplicationDate(LocalDateTime.now());
        jobApplication = jobApplicationRepository.save(jobApplication);

        SecurityContextHolder.clearContext(); // Clear context for each test
    }

    // Helper method to authenticate with roles
    private void authenticateAs(UUID userId, Role role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name())))
        );
    }

    @Test
    void transitionApplicationStatus_asHiringManager_validTransition_shouldReturnOk() throws Exception {
        authenticateAs(hiringManager.getId(), Role.HIRING_MANAGER);
        ApplicationTransitionRequest request = new ApplicationTransitionRequest(JobApplicationStatus.UNDER_REVIEW);

        mockMvc.perform(post("/api/applications/{applicationId}/transition", jobApplication.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(JobApplicationStatus.UNDER_REVIEW.name()));

        JobApplication updatedApplication = jobApplicationRepository.findById(jobApplication.getId()).orElseThrow();
        assertThat(updatedApplication.getStatus()).isEqualTo(JobApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void transitionApplicationStatus_asRecruiter_validTransition_shouldReturnOk() throws Exception {
        authenticateAs(recruiter.getId(), Role.RECRUITER);
        ApplicationTransitionRequest request = new ApplicationTransitionRequest(JobApplicationStatus.UNDER_REVIEW);

        mockMvc.perform(post("/api/applications/{applicationId}/transition", jobApplication.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(JobApplicationStatus.UNDER_REVIEW.name()));

        JobApplication updatedApplication = jobApplicationRepository.findById(jobApplication.getId()).orElseThrow();
        assertThat(updatedApplication.getStatus()).isEqualTo(JobApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void transitionApplicationStatus_asCandidate_shouldReturnForbidden() throws Exception {
        authenticateAs(candidate.getId(), Role.CANDIDATE);
        ApplicationTransitionRequest request = new ApplicationTransitionRequest(JobApplicationStatus.UNDER_REVIEW);

        mockMvc.perform(post("/api/applications/{applicationId}/transition", jobApplication.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transitionApplicationStatus_invalidApplicationId_shouldReturnNotFound() throws Exception {
        authenticateAs(hiringManager.getId(), Role.HIRING_MANAGER);
        UUID nonExistentId = UUID.randomUUID();
        ApplicationTransitionRequest request = new ApplicationTransitionRequest(JobApplicationStatus.UNDER_REVIEW);

        mockMvc.perform(post("/api/applications/{applicationId}/transition", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transitionApplicationStatus_invalidTransition_shouldReturnBadRequest() throws Exception {
        authenticateAs(hiringManager.getId(), Role.HIRING_MANAGER);
        // Set status to a terminal state to make further transitions invalid
        jobApplication.setStatus(JobApplicationStatus.HIRED);
        jobApplicationRepository.save(jobApplication);

        ApplicationTransitionRequest request = new ApplicationTransitionRequest(JobApplicationStatus.UNDER_REVIEW);

        mockMvc.perform(post("/api/applications/{applicationId}/transition", jobApplication.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transitionApplicationStatus_unauthorizedOrganization_shouldReturnForbidden() throws Exception {
        // Create a hiring manager from a different organization
        User otherHiringManager = testHelper.createUser("otherhm@test.com", Role.HIRING_MANAGER);
        authenticateAs(otherHiringManager.getId(), Role.HIRING_MANAGER);

        ApplicationTransitionRequest request = new ApplicationTransitionRequest(JobApplicationStatus.UNDER_REVIEW);

        mockMvc.perform(post("/api/applications/{applicationId}/transition", jobApplication.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void transitionApplicationStatus_missingTargetStatus_shouldReturnBadRequest() throws Exception {
        authenticateAs(hiringManager.getId(), Role.HIRING_MANAGER);

        // Create a request with a null targetStatus
        String invalidJson = "{\"targetStatus\": null}";

        mockMvc.perform(post("/api/applications/{applicationId}/transition", jobApplication.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
