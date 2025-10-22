package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobApplicationRepository;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicantControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User hiringManager;
    private User recruiter;
    private User candidate1;
    private User candidate2;
    private JobPost jobPost;
    private Organization organization;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();

        // Setup organization and users
        hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);
        organization = hiringManager.getOrganization();

        recruiter = testHelper.createUser("recruiter@test.com", Role.RECRUITER);
        recruiter.setOrganization(organization);
        recruiter = userRepository.save(recruiter);

        // Create candidates with different profiles
        candidate1 = createCandidateWithProfile("candidate1@test.com", "Alice", "Smith", 5, "Bachelor", "New York");
        candidate2 = createCandidateWithProfile("candidate2@test.com", "Bob", "Johnson", 3, "Master", "San Francisco");

        // Create job post
        authenticateAsWithRoles(hiringManager);
        jobPost = createJobPost("Senior Java Developer", organization);

        // Create applications
        createApplication(candidate1, jobPost, JobApplicationStatus.APPLIED);
        createApplication(candidate2, jobPost, JobApplicationStatus.UNDER_REVIEW);
    }

    @Test
    void getApplicants_asHiringManager_shouldReturnAllApplications() throws Exception {
        // Given
        authenticateAsWithRoles(hiringManager);

        // When & Then
        mockMvc.perform(get("/api/applicants")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getApplicants_asRecruiter_shouldReturnAllApplications() throws Exception {
        // Given
        authenticateAsWithRoles(recruiter);

        // When & Then
        mockMvc.perform(get("/api/applicants")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getApplicants_asCandidate_shouldReturnForbidden() throws Exception {
        // Given
        authenticateAsWithRoles(candidate1);

        // When & Then
        mockMvc.perform(get("/api/applicants")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void getApplicants_withSearchFilter_shouldReturnMatchingResults() throws Exception {
        // Given
        authenticateAsWithRoles(hiringManager);

        // When & Then
        mockMvc.perform(get("/api/applicants")
                .param("search", "Alice")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].candidateName").value(containsString("Alice")));
    }

    // Helper methods
    private User createCandidateWithProfile(String email, String firstName, String lastName, Integer experience,
                                           String education, String location) {
        User candidate = testHelper.createUser(email, Role.CANDIDATE);
        candidate.setFirstName(firstName);
        candidate.setLastName(lastName);

        ObjectNode profile = objectMapper.createObjectNode();
        profile.put("experienceYears", experience);

        ArrayNode educationArray = objectMapper.createArrayNode();
        ObjectNode eduNode = objectMapper.createObjectNode();
        eduNode.put("degree", education);
        educationArray.add(eduNode);
        profile.set("education", educationArray);

        candidate.setProfile(profile);
        return userRepository.save(candidate);
    }

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

        ObjectNode location = objectMapper.createObjectNode();
        location.put("city", "New York");
        post.setLocation(location);

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
