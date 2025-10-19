package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.WorkflowTriggerRequest;
import com.etalente.backend.dto.WorkflowTriggerResponse;
import com.etalente.backend.integration.novu.NovuWorkflowService;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JobApplicationControllerTest extends BaseIntegrationTest {

    @MockBean
    private NovuWorkflowService novuWorkflowService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobApplicationAuditRepository jobApplicationAuditRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getMyApplications_shouldReturn403_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/applications/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyApplications_shouldReturn200_whenAuthenticated() throws Exception {
        String candidateToken = testHelper.createUserAndGetJwt("candidate-200@test.com", Role.CANDIDATE);
        mockMvc.perform(get("/api/applications/me")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());
    }

    @Test
    void getMyApplications_shouldReturnFilteredResults_whenSearchQueryIsProvided() throws Exception {
        // Given
        User candidate = testHelper.createUser("candidate-search@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.generateJwtForUser(candidate);
        User hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);

        JobPost jobPost1 = new JobPost();
        jobPost1.setTitle("Java Developer");
        jobPost1.setCompany("Acme Inc.");
        jobPost1.setCreatedBy(hiringManager);
        jobPostRepository.save(jobPost1);

        JobPost jobPost2 = new JobPost();
        jobPost2.setTitle("Python Developer");
        jobPost2.setCompany("Beta Corp.");
        jobPost2.setCreatedBy(hiringManager);
        jobPostRepository.save(jobPost2);

        JobApplication application1 = new JobApplication();
        application1.setCandidate(candidate);
        application1.setJobPost(jobPost1);
        application1.setStatus(JobApplicationStatus.APPLIED);
        jobApplicationRepository.save(application1);

        JobApplication application2 = new JobApplication();
        application2.setCandidate(candidate);
        application2.setJobPost(jobPost2);
        application2.setStatus(JobApplicationStatus.APPLIED);
        jobApplicationRepository.save(application2);

        // When & Then
        mockMvc.perform(get("/api/applications/me")
                        .header("Authorization", "Bearer " + candidateToken)
                        .param("search", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].job.title", is("Java Developer")));
    }

    @Test
    void getApplicationDetails_shouldReturnApplicationDetailsWithCommunicationHistory() throws Exception {
        // Given
        User candidate = testHelper.createUser("candidate-details@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.generateJwtForUser(candidate);
        User hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);

        JobPost jobPost1 = new JobPost();
        jobPost1.setTitle("Java Developer");
        jobPost1.setCompany("Acme Inc.");
        jobPost1.setCreatedBy(hiringManager);
        jobPostRepository.save(jobPost1);

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJobPost(jobPost1);
        application.setStatus(JobApplicationStatus.APPLIED);
        application = jobApplicationRepository.save(application);

        jobApplicationAuditRepository.save(new JobApplicationAudit(application, JobApplicationStatus.APPLIED, "Application submitted."));

        // When & Then
        mockMvc.perform(get("/api/applications/{id}", application.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communicationHistory.length()", is(1)))
                .andExpect(jsonPath("$.communicationHistory[0].status", is("APPLIED")));

    }

    @Test
    void getMyApplications_shouldReturnSortedResults_whenSortQueryIsProvided() throws Exception {
        // Given
        User candidate = testHelper.createUser("candidate-sort@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.generateJwtForUser(candidate);
        User hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);

        JobPost jobPost1 = new JobPost();
        jobPost1.setTitle("Java Developer");
        jobPost1.setCompany("Acme Inc.");
        jobPost1.setCreatedBy(hiringManager);
        jobPostRepository.save(jobPost1);

        JobPost jobPost2 = new JobPost();
        jobPost2.setTitle("Python Developer");
        jobPost2.setCompany("Beta Corp.");
        jobPost2.setCreatedBy(hiringManager);
        jobPostRepository.save(jobPost2);

        JobApplication application1 = new JobApplication();
        application1.setCandidate(candidate);
        application1.setJobPost(jobPost1);
        application1.setStatus(JobApplicationStatus.APPLIED);
        jobApplicationRepository.save(application1);

        JobApplication application2 = new JobApplication();
        application2.setCandidate(candidate);
        application2.setJobPost(jobPost2);
        application2.setStatus(JobApplicationStatus.INTERVIEW_SCHEDULED);
        jobApplicationRepository.save(application2);

        // When & Then
        mockMvc.perform(get("/api/applications/me")
                        .header("Authorization", "Bearer " + candidateToken)
                        .param("sort", "status,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].status", is("APPLIED")));
    }
    @Test
    void applyForJob_shouldCreateApplicationAndTriggerNotification() throws Exception {
        // Given
        String candidateToken = testHelper.createUserAndGetJwt("candidate-apply-success@test.com", Role.CANDIDATE);
        User hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);

        JobPost jobPost = new JobPost();
        jobPost.setTitle("Open Job for Application");
        jobPost.setCompany("Novu Test Corp.");
        jobPost.setCreatedBy(hiringManager);
        jobPost.setStatus(JobPostStatus.OPEN); // Must be OPEN to apply
        jobPostRepository.save(jobPost);

        // Mock NovuWorkflowService to return a completed future
        when(novuWorkflowService.triggerWorkflow(any(String.class), any(WorkflowTriggerRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(new WorkflowTriggerResponse()));

        // When
        mockMvc.perform(post("/api/job-posts/{id}/apply", jobPost.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.job.title", is("Open Job for Application")));

        // Then
        assertThat(jobApplicationRepository.count()).isEqualTo(1L);
        verify(novuWorkflowService, times(1)).triggerWorkflow(eq("application-received"), any(WorkflowTriggerRequest.class));
    }

    @Test
    void applyForJob_shouldFail_whenJobPostIsNotOpen() throws Exception {
        // Given
        String candidateToken = testHelper.createUserAndGetJwt("candidate-apply-fail@test.com", Role.CANDIDATE);
        User hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);

        JobPost jobPost = new JobPost();
        jobPost.setTitle("Closed Job");
        jobPost.setCompany("Closed Corp.");
        jobPost.setCreatedBy(hiringManager);
        jobPost.setStatus(JobPostStatus.CLOSED); // Set status to CLOSED
        jobPostRepository.save(jobPost);

        // When & Then
        mockMvc.perform(post("/api/job-posts/{id}/apply", jobPost.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Cannot apply for a job that is not OPEN.")));

    }

    @Test
    void withdrawApplication_shouldWithdrawSuccessfully_whenStatusIsApplied() throws Exception {
        // Given
        User candidate = testHelper.createUser("candidate-withdraw-success@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.generateJwtForUser(candidate);
        User hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);

        JobPost jobPost = new JobPost();
        jobPost.setTitle("Test Job");
        jobPost.setCompany("Test Company");
        jobPost.setCreatedBy(hiringManager);
        jobPostRepository.save(jobPost);

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJobPost(jobPost);
        application.setStatus(JobApplicationStatus.APPLIED);
        application = jobApplicationRepository.save(application);

        // When & Then
        mockMvc.perform(delete("/api/applications/{id}", application.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isNoContent());

        JobApplication updatedApplication = jobApplicationRepository.findById(application.getId()).orElseThrow();
        assertThat(updatedApplication.getStatus()).isEqualTo(JobApplicationStatus.WITHDRAWN);
    }

    @Test
    void withdrawApplication_shouldFail_whenStatusIsNotAppliedOrUnderReview() throws Exception {
        // Given
        User candidate = testHelper.createUser("candidate-withdraw-fail@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.generateJwtForUser(candidate);
        User hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);

        JobPost jobPost = new JobPost();
        jobPost.setTitle("Test Job");
        jobPost.setCompany("Test Company");
        jobPost.setCreatedBy(hiringManager);
        jobPostRepository.save(jobPost);

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJobPost(jobPost);
        application.setStatus(JobApplicationStatus.HIRED); // Invalid status for withdrawal
        application = jobApplicationRepository.save(application);

        // When & Then
        mockMvc.perform(delete("/api/applications/{id}", application.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Application can only be withdrawn if its status is APPLIED or UNDER_REVIEW.")));
    }

    @Test
    void getApplicationDetails_shouldReturnCandidateProfile() throws Exception {
        // Given
        User candidate = testHelper.createUser("candidate-profile@test.com", Role.CANDIDATE);
        candidate.setProfile(objectMapper.readTree("{\"basics\":{\"name\":\"John Doe\"}}"));
        userRepository.save(candidate);
        entityManager.flush();
        String candidateToken = testHelper.generateJwtForUser(candidate);
        User hiringManager = testHelper.createUser("hm-profile@test.com", Role.HIRING_MANAGER);

        JobPost jobPost1 = new JobPost();
        jobPost1.setTitle("Java Developer");
        jobPost1.setCompany("Acme Inc.");
        jobPost1.setCreatedBy(hiringManager);
        jobPostRepository.save(jobPost1);

        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJobPost(jobPost1);
        application.setStatus(JobApplicationStatus.APPLIED);
        application = jobApplicationRepository.save(application);

        // When & Then
        mockMvc.perform(get("/api/applications/{id}", application.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateProfile.firstName", is(candidate.getFirstName())))
                .andExpect(jsonPath("$.candidateProfile.profile.basics.name", is("John Doe")));
    }

    @Test
    void getApplicationsForJob_shouldReturnApplications_whenOwner() throws Exception {
        // Given
        User hiringManager = testHelper.createUser("hm-owner@test.com", Role.HIRING_MANAGER);
        hiringManager = userRepository.findById(hiringManager.getId()).orElseThrow();
        String hiringManagerJwt = testHelper.generateJwtForUser(hiringManager);
        JobPost jobPost = new JobPost();
        jobPost.setTitle("Test Job Post");
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPost.setCreatedBy(hiringManager);
        jobPost.setOrganization(hiringManager.getOrganization()); // Add this too!
        jobPost = jobPostRepository.save(jobPost);

        User candidate1 = testHelper.createUser("candidate1@example.com", Role.CANDIDATE);
        candidate1 = userRepository.findById(candidate1.getId()).orElseThrow();
        User candidate2 = testHelper.createUser("candidate2@example.com", Role.CANDIDATE);
        candidate2 = userRepository.findById(candidate2.getId()).orElseThrow();

        JobApplication app1 = new JobApplication();
        app1.setJobPost(jobPost);
        app1.setCandidate(candidate1);
        app1.setStatus(JobApplicationStatus.APPLIED);
        jobApplicationRepository.save(app1);

        JobApplication app2 = new JobApplication();
        app2.setJobPost(jobPost);
        app2.setCandidate(candidate2);
        app2.setStatus(JobApplicationStatus.APPLIED);
        jobApplicationRepository.save(app2); // ADD THIS LINE!

        // When & Then
        mockMvc.perform(get("/api/job-posts/{jobId}/applications", jobPost.getId())
                        .header("Authorization", "Bearer " + hiringManagerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getApplicationsForJob_shouldReturnForbidden_whenNotOwner() throws Exception {
        // Given
        User owner = testHelper.createUser("owner@example.com", Role.HIRING_MANAGER);
        JobPost jobPost = new JobPost();
        jobPost.setCreatedBy(owner);
        jobPostRepository.save(jobPost);

        User otherHiringManager = testHelper.createUser("otherhm@example.com", Role.HIRING_MANAGER);
        String otherJwt = testHelper.generateJwtForUser(otherHiringManager);

        // When & Then
        mockMvc.perform(get("/api/job-posts/{jobId}/applications", jobPost.getId())
                        .header("Authorization", "Bearer " + otherJwt))
                .andExpect(status().isForbidden());
    }
}