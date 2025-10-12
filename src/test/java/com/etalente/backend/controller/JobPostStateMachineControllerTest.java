package com.etalente.backend.controller;

import com.etalente.backend.dto.StateTransitionRequest;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.JobPostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobPostStateMachineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JobPostService jobPostService;

    private String hiringManagerToken;
    private String recruiterToken;
    private User hiringManager;
    private User recruiter;
    private Organization organization;
    private JobPost testJobPost;

    @BeforeEach
    void setUp() {
        // Create organization
        organization = new Organization();
        organization.setName("Test Company " + UUID.randomUUID());
        organization = organizationRepository.save(organization);

        // Create hiring manager
        hiringManager = new User();
        hiringManager.setEmail("hm-" + UUID.randomUUID() + "@test.com");
        hiringManager.setRole(Role.HIRING_MANAGER);
        hiringManager.setOrganization(organization);
        hiringManager.setEmailVerified(true);
        hiringManager = userRepository.save(hiringManager);

        // Create recruiter
        recruiter = new User();
        recruiter.setEmail("recruiter-" + UUID.randomUUID() + "@test.com");
        recruiter.setRole(Role.RECRUITER);
        recruiter.setOrganization(organization);
        recruiter.setEmailVerified(true);
        recruiter = userRepository.save(recruiter);

        // Create test job post
        testJobPost = createCompleteJobPost();
        testJobPost = jobPostRepository.save(testJobPost);

        // Flush to ensure everything is persisted
        entityManager.flush();
        entityManager.clear();

        // Reload entities to ensure they're managed and have proper IDs
        organization = organizationRepository.findById(organization.getId()).orElseThrow();
        hiringManager = userRepository.findById(hiringManager.getId()).orElseThrow();
        recruiter = userRepository.findById(recruiter.getId()).orElseThrow();
        testJobPost = jobPostRepository.findById(testJobPost.getId()).orElseThrow();

        // Generate tokens AFTER flushing and reloading
        UserDetails hiringManagerDetails = userDetailsService.loadUserByUsername(hiringManager.getEmail());
        hiringManagerToken = jwtService.generateToken(hiringManagerDetails);

        UserDetails recruiterDetails = userDetailsService.loadUserByUsername(recruiter.getEmail());
        recruiterToken = jwtService.generateToken(recruiterDetails);
    }

    @Test
    void testPublishJobPost_Success() throws Exception {
        testJobPost = jobPostRepository.findById(testJobPost.getId()).orElseThrow();
        mockMvc.perform(patch("/api/job-posts/{id}/publish", testJobPost.getId())
                        .header("Authorization", "Bearer " + hiringManagerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void testPublishIncompleteJobPost_Fails() throws Exception {
        testJobPost.setTitle(null);
        testJobPost = jobPostRepository.save(testJobPost);
        entityManager.flush();

        mockMvc.perform(patch("/api/job-posts/{id}/publish", testJobPost.getId())
                        .header("Authorization", "Bearer " + hiringManagerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("missing required fields")));
    }

    @Test
    void testCloseJobPost_WithReason() throws Exception {
        // First publish
        testJobPost.setStatus(JobPostStatus.OPEN);
        testJobPost = jobPostRepository.save(testJobPost);
        entityManager.flush();

        mockMvc.perform(patch("/api/job-posts/{id}/close", testJobPost.getId())
                        .param("reason", "Position filled")
                        .header("Authorization", "Bearer " + hiringManagerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void testReopenJobPost() throws Exception {
        // Set to closed
        testJobPost.setStatus(JobPostStatus.CLOSED);
        testJobPost = jobPostRepository.save(testJobPost);
        entityManager.flush();

        mockMvc.perform(patch("/api/job-posts/{id}/reopen", testJobPost.getId())
                        .param("reason", "Need to hire more candidates")
                        .header("Authorization", "Bearer " + hiringManagerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void testArchiveJobPost() throws Exception {
        testJobPost.setStatus(JobPostStatus.CLOSED);
        testJobPost = jobPostRepository.save(testJobPost);
        entityManager.flush();

        mockMvc.perform(patch("/api/job-posts/{id}/archive", testJobPost.getId())
                        .param("reason", "Archiving old posting")
                        .header("Authorization", "Bearer " + hiringManagerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void testGenericTransitionEndpoint() throws Exception {
        StateTransitionRequest request = new StateTransitionRequest(
                JobPostStatus.OPEN,
                "Publishing via generic endpoint"
        );

        mockMvc.perform(patch("/api/job-posts/{id}/transition", testJobPost.getId())
                        .header("Authorization", "Bearer " + hiringManagerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void testInvalidTransition_Fails() throws Exception {
        testJobPost.setStatus(JobPostStatus.ARCHIVED);
        testJobPost = jobPostRepository.save(testJobPost);
        entityManager.flush();

        StateTransitionRequest request = new StateTransitionRequest(
                JobPostStatus.OPEN,
                "Trying invalid transition"
        );

        mockMvc.perform(patch("/api/job-posts/{id}/transition", testJobPost.getId())
                        .header("Authorization", "Bearer " + hiringManagerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid state transition")));
    }

    @Test
    void testRecruiterCanPublish() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/publish", testJobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void testRecruiterCanClose() throws Exception {
        testJobPost.setStatus(JobPostStatus.OPEN);
        testJobPost = jobPostRepository.save(testJobPost);
        entityManager.flush();

        mockMvc.perform(patch("/api/job-posts/{id}/close", testJobPost.getId())
                        .param("reason", "Closed by recruiter")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void testTransitionToSameStatus_Fails() throws Exception {
        testJobPost.setStatus(JobPostStatus.OPEN);
        testJobPost = jobPostRepository.save(testJobPost);
        entityManager.flush();

        StateTransitionRequest request = new StateTransitionRequest(
                JobPostStatus.OPEN,
                "Same status"
        );

        mockMvc.perform(patch("/api/job-posts/{id}/transition", testJobPost.getId())
                        .header("Authorization", "Bearer " + hiringManagerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already in OPEN status")));
    }

    @Test
    void testUnauthorizedAccess_Fails() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/publish", testJobPost.getId()))
                .andExpect(status().isForbidden()); // Changed from isUnauthorized to isForbidden
    }

    @Test
    void testBeansExist() {
        assertNotNull(jobPostService, "JobPostService should be injected");
        System.out.println("JobPostService class: " + jobPostService.getClass().getName());
    }

    private JobPost createCompleteJobPost() {
        JobPost jobPost = new JobPost();
        jobPost.setTitle("Software Engineer");
        jobPost.setDescription("Test description for a software engineer position");
        jobPost.setJobType("Full-time");
        jobPost.setExperienceLevel("Mid-level");
        jobPost.setCompany(organization.getName());

        // Create a proper location
        ObjectNode location = objectMapper.createObjectNode();
        location.put("city", "San Francisco");
        location.put("countryCode", "US");
        jobPost.setLocation(location);

        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost.setCreatedBy(hiringManager);
        jobPost.setOrganization(organization);
        return jobPost;
    }
}
