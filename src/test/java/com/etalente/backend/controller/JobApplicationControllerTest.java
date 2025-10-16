package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.WorkflowTriggerRequest;
import com.etalente.backend.dto.WorkflowTriggerResponse;
import com.etalente.backend.integration.novu.NovuWorkflowService;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.*;
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

    @Test
    void getMyApplications_shouldReturn403_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/applications/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyApplications_shouldReturn200_whenAuthenticated() throws Exception {
        String candidateToken = testHelper.createUserAndGetJwt("candidate@test.com", Role.CANDIDATE);
        mockMvc.perform(get("/api/applications/me")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());
    }

    @Test
    void getMyApplications_shouldReturnFilteredResults_whenSearchQueryIsProvided() throws Exception {
        // Given
        User candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.createUserAndGetJwt(candidate.getEmail(), candidate.getRole());
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
        User candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.createUserAndGetJwt(candidate.getEmail(), candidate.getRole());
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
        jobApplicationRepository.save(application);

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
        User candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.createUserAndGetJwt(candidate.getEmail(), candidate.getRole());
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
                        .param("sort", "status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].status", is("APPLIED")));
    }
    @Test
    void applyForJob_shouldCreateApplicationAndTriggerNotification() throws Exception {
        // Given
        String candidateToken = testHelper.createUserAndGetJwt("candidate@test.com", Role.CANDIDATE);
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
        String candidateToken = testHelper.createUserAndGetJwt("candidate@test.com", Role.CANDIDATE);
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
        User candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.createUserAndGetJwt(candidate.getEmail(), candidate.getRole());
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
        jobApplicationRepository.save(application);

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
        User candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);
        String candidateToken = testHelper.createUserAndGetJwt(candidate.getEmail(), candidate.getRole());
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
        jobApplicationRepository.save(application);

        // When & Then
        mockMvc.perform(delete("/api/applications/{id}", application.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Application can only be withdrawn if its status is APPLIED or UNDER_REVIEW.")));
    }
}