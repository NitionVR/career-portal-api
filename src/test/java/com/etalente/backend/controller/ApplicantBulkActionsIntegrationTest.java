package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.BulkStatusUpdateRequest;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobApplicationRepository;
import com.etalente.backend.repository.JobPostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicantBulkActionsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User hiringManager;
    private User recruiter;
    private Organization organization;
    private List<JobApplication> applications;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();

        hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);
        organization = hiringManager.getOrganization();

        recruiter = testHelper.createUser("recruiter@test.com", Role.RECRUITER);
        recruiter.setOrganization(organization);

        // Create 10 test applications
        applications = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User candidate = testHelper.createUser("candidate" + i + "@test.com", Role.CANDIDATE);
            JobPost jobPost = createJobPost("Job " + i, organization);
            JobApplication app = createApplication(candidate, jobPost, JobApplicationStatus.APPLIED);
            applications.add(app);
        }
    }

    @Test
    void bulkUpdateStatus_asHiringManager_shouldUpdateAll() throws Exception {
        // Given
        authenticateAsWithRoles(hiringManager);

        List<UUID> appIds = applications.stream()
            .limit(5)
            .map(JobApplication::getId)
            .collect(Collectors.toList());

        BulkStatusUpdateRequest request = new BulkStatusUpdateRequest();
        request.setApplicationIds(appIds);
        request.setTargetStatus(JobApplicationStatus.UNDER_REVIEW);
        request.setSendNotification(false);

        // When & Then
        mockMvc.perform(post("/api/applicants/bulk-update-status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequested").value(5))
                .andExpect(jsonPath("$.successCount").value(5))
                .andExpect(jsonPath("$.failureCount").value(0))
                .andExpect(jsonPath("$.errors").isEmpty());

        // Verify database updates
        List<JobApplication> updated = jobApplicationRepository.findAllById(appIds);
        assertThat(updated).allMatch(app ->
            app.getStatus() == JobApplicationStatus.UNDER_REVIEW
        );
    }

    // Helper methods
    private JobPost createJobPost(String title, Organization org) {
        JobPost post = new JobPost();
        post.setTitle(title);
        post.setCompany(org.getName());
        post.setJobType("Full-time");
        post.setDescription("Test description");
        post.setRemote("Remote");
        post.setExperienceLevel("Senior");
        post.setStatus(JobPostStatus.OPEN);
        post.setCreatedBy(hiringManager);
        post.setOrganization(org);
        return jobPostRepository.save(post);
    }

    private JobApplication createApplication(User candidate, JobPost jobPost,
                                            JobApplicationStatus status) {
        JobApplication application = new JobApplication();
        application.setCandidate(candidate);
        application.setJobPost(jobPost);
        application.setStatus(status);
        application.setApplicationDate(LocalDateTime.now());
        return jobApplicationRepository.save(application);
    }

    protected void authenticateAsWithRoles(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getId().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
