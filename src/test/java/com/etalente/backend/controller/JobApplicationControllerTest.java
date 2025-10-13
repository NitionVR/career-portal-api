package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.*;
import com.etalente.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;

public class JobApplicationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobApplicationAuditRepository jobApplicationAuditRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JwtService jwtService;

    private User candidate;
    private User hiringManager;
    private String candidateToken;

    @BeforeEach
    void setUp() {
        Organization org = new Organization();
        org.setName("Test Corp");
        organizationRepository.save(org);

        candidate = new User();
        candidate.setEmail("candidate@test.com");
        candidate.setUsername("candidate");
        candidate.setRole(Role.CANDIDATE);
        candidate.setOrganization(null);
        userRepository.save(candidate);

        hiringManager = new User();
        hiringManager.setEmail("hm@test.com");
        hiringManager.setUsername("hm");
        hiringManager.setRole(Role.HIRING_MANAGER);
        hiringManager.setOrganization(org);
        userRepository.save(hiringManager);

        candidateToken = jwtService.generateToken(userDetailsService.loadUserByUsername(candidate.getEmail()));
    }

    @Test
    void getMyApplications_shouldReturn403_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/applications/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMyApplications_shouldReturn200_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/applications/me")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk());
    }

    @Test
    void getMyApplications_shouldReturnFilteredResults_whenSearchQueryIsProvided() throws Exception {
        // Given
        JobPost jobPost1 = new JobPost();
        jobPost1.setTitle("Java Developer");
        jobPost1.setCompany("Acme Inc.");
        jobPost1.setCreatedBy(hiringManager); // Set the createdBy user
        jobPostRepository.save(jobPost1);

        JobPost jobPost2 = new JobPost();
        jobPost2.setTitle("Python Developer");
        jobPost2.setCompany("Beta Corp.");
        jobPost2.setCreatedBy(hiringManager); // Set the createdBy user
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
}
