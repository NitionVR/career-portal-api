package com.etalente.backend.controller;

import com.etalente.backend.TestHelper;
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

    @Autowired
    private TestHelper testHelper;

    private JobPost completeJobPost;
    private User hm;
    private User recruiter;
    private String hmToken;
    private String recruiterToken;
    private JobPost incompleteJobPost;
    private JobPost openJobPost;
    private JobPost closedJobPost;

    @BeforeEach
    void setUp() {
        hm = testHelper.createUser("hm-" + UUID.randomUUID() + "@test.com", Role.HIRING_MANAGER);
        recruiter = testHelper.createUser("recruiter-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
        recruiter.setOrganization(hm.getOrganization());
        userRepository.save(recruiter);

        hmToken = testHelper.generateJwtForUser(hm);
        recruiterToken = testHelper.generateJwtForUser(recruiter);

        completeJobPost = createCompleteJobPost(hm);
        incompleteJobPost = createIncompleteJobPost(hm);

        openJobPost = createCompleteJobPost(hm);
        openJobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(openJobPost);

        closedJobPost = createCompleteJobPost(hm);
        closedJobPost.setStatus(JobPostStatus.CLOSED);
        jobPostRepository.save(closedJobPost);
    }

    @Test
    void testPublishJobPost_Success() throws Exception {
        User hiringManager = testHelper.createUser("hm-" + UUID.randomUUID() + "@test.com", Role.HIRING_MANAGER);
        String hmToken = testHelper.generateJwtForUser(hiringManager);
        JobPost jobPost = createCompleteJobPost(hiringManager);

        mockMvc.perform(patch("/api/job-posts/{id}/publish", jobPost.getId())
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void testPublishIncompleteJobPost_Fails() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/publish", incompleteJobPost.getId())
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCloseJobPost_WithReason() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/close", openJobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .param("reason", "Position filled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void testReopenJobPost() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/reopen", closedJobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .param("reason", "Need to hire more candidates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void testArchiveJobPost() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/archive", completeJobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .param("reason", "Archiving old posting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void testGenericTransitionEndpoint() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/transition", completeJobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .contentType("application/json")
                        .content("{\"targetStatus\":\"OPEN\",\"reason\":\"Publishing via generic endpoint\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void testInvalidTransition_Fails() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/transition", closedJobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .contentType("application/json")
                        .content("{\"targetStatus\":\"DRAFT\",\"reason\":\"Trying invalid transition\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testRecruiterCanPublish() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/publish", completeJobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void testRecruiterCanClose() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/close", openJobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken)
                        .param("reason", "Closed by recruiter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void testTransitionToSameStatus_Fails() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/transition", openJobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .contentType("application/json")
                        .content("{\"targetStatus\":\"OPEN\",\"reason\":\"Same status\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnauthorizedAccess_Fails() throws Exception {
        mockMvc.perform(patch("/api/job-posts/{id}/publish", completeJobPost.getId()))
                .andExpect(status().isForbidden()); // Changed from isUnauthorized to isForbidden
    }

    @Test
    void testBeansExist() {
        assertNotNull(jobPostService, "JobPostService should be injected");
        System.out.println("JobPostService class: " + jobPostService.getClass().getName());
    }

    private JobPost createIncompleteJobPost(User user) {
        JobPost jobPost = new JobPost();
        jobPost.setTitle("Incomplete Job");
        jobPost.setCreatedBy(user);
        jobPost.setOrganization(user.getOrganization());
        return jobPostRepository.save(jobPost);
    }

    private JobPost createCompleteJobPost(User user) {
        JobPost jobPost = new JobPost();
        jobPost.setTitle("Software Engineer");
        jobPost.setDescription("Test description for a software engineer position");
        jobPost.setJobType("Full-time");
        jobPost.setExperienceLevel("Mid-level");
        jobPost.setCompany(user.getOrganization().getName());

        // Create a proper location
        ObjectNode location = objectMapper.createObjectNode();
        location.put("city", "San Francisco");
        location.put("countryCode", "US");
        jobPost.setLocation(location);

        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost.setCreatedBy(user);
        jobPost.setOrganization(user.getOrganization());
        return jobPostRepository.save(jobPost);
    }
}
