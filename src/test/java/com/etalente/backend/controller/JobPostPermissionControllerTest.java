package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.SkillDto;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class JobPostPermissionControllerTest extends BaseIntegrationTest {



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

    private Organization organization;
    private User hiringManager;
    private User recruiter;
    private User candidate;
    private String hmToken;
    private String recruiterToken;
    private String candidateToken;
    private JobPost jobPost;

    @BeforeEach
    void setUp() {
        // Create organization
        organization = new Organization();
        organization.setName("Test Company " + UUID.randomUUID());
        organization.setIndustry("Tech");
        organization = organizationRepository.save(organization);

        // Create users with unique emails
        hiringManager = createUser("hm-" + UUID.randomUUID() + "@test.com", "hm1", Role.HIRING_MANAGER, organization);
        recruiter = createUser("recruiter-" + UUID.randomUUID() + "@test.com", "rec1", Role.RECRUITER, organization);
        candidate = createUser("candidate-" + UUID.randomUUID() + "@test.com", "cand1", Role.CANDIDATE, null);

        // Create job post with ALL required fields for publishing
        jobPost = new JobPost();
        jobPost.setTitle("Test Job");
        jobPost.setDescription("This is a test description for a job post. It needs to be at least 50 characters long to pass validation.");
        jobPost.setJobType("Full-time");
        jobPost.setExperienceLevel("Mid-level");
        jobPost.setCompany(organization.getName());

        ObjectNode location = objectMapper.createObjectNode();
        location.put("city", "San Francisco");
        location.put("countryCode", "US");
        jobPost.setLocation(location);

        jobPost.setCreatedBy(hiringManager);
        jobPost.setOrganization(organization);
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost = jobPostRepository.save(jobPost);

        // Flush to ensure everything is persisted
        entityManager.flush();
        entityManager.clear();

        // Reload entities to ensure they're managed and have proper IDs
        organization = organizationRepository.findById(organization.getId()).orElseThrow();
        hiringManager = userRepository.findById(hiringManager.getId()).orElseThrow();
        recruiter = userRepository.findById(recruiter.getId()).orElseThrow();
        candidate = userRepository.findById(candidate.getId()).orElseThrow();
        jobPost = jobPostRepository.findById(jobPost.getId()).orElseThrow();

        // Generate tokens AFTER flushing and reloading
        UserDetails hmDetails = userDetailsService.loadUserByUsername(hiringManager.getEmail());
        hmToken = jwtService.generateToken(hmDetails);
        UserDetails recruiterDetails = userDetailsService.loadUserByUsername(recruiter.getEmail());
        recruiterToken = jwtService.generateToken(recruiterDetails);
        UserDetails candidateDetails = userDetailsService.loadUserByUsername(candidate.getEmail());
        candidateToken = jwtService.generateToken(candidateDetails);
    }



    // === CREATE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanCreateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(request.title()));
    }

    @Test
    void recruiterCannotCreateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCannotCreateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // === UPDATE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanUpdateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void recruiterCanUpdateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void candidateCannotUpdateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // === DELETE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanDeleteOwnJobPost() throws Exception {
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void recruiterCannotDeleteJobPost() throws Exception {
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCannotDeleteJobPost() throws Exception {
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    // === STATUS CHANGE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanPublishJobPost() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void recruiterCanPublishJobPost() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void candidateCannotPublishJobPost() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hiringManagerCanCloseJobPost() throws Exception {
        // First publish it
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/close")
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void recruiterCanCloseJobPost() throws Exception {
        // First publish it
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/close")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void candidateCannotCloseJobPost() throws Exception {
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/close")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hiringManagerCanUpdateJobPostStatus() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/status")
                        .header("Authorization", "Bearer " + hmToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void recruiterCanUpdateJobPostStatus() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/status")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    // === LISTING ENDPOINT TESTS ===

    @Test
    void recruiterCanAccessMyPostsEndpoint() throws Exception {
        mockMvc.perform(get("/api/job-posts/my-posts")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk());
    }

    @Test
    void candidateCannotAccessMyPostsEndpoint() throws Exception {
        mockMvc.perform(get("/api/job-posts/my-posts")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessProtectedEndpoints() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCanAccessPublicJobPost() throws Exception {
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(get("/api/job-posts/" + jobPost.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCanListPublicJobPosts() throws Exception {
        mockMvc.perform(get("/api/job-posts"))
                .andExpect(status().isOk());
    }

    // Helper methods
    private User createUser(String email, String username, Role role, Organization org) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setOrganization(org);
        user.setEmailVerified(true);
        user.setProfileComplete(true);
        return userRepository.save(user);
    }

    private JobPostRequest createJobPostRequest() {
        return new JobPostRequest(
                "Software Engineer",
                "Test Company",
                "Full-time",
                "This is a test description for a job post. It needs to be at least 50 characters long to pass validation.",
                new LocationDto("123 Main St", "12345", "San Francisco", "US", "CA"),
                "Hybrid",
                "$100k-$150k",
                "Senior-level",  // CHANGE THIS from "Senior" to "Senior-level"
                List.of("Develop features", "Code reviews", "Mentor juniors"),  // Add more items
                List.of("5+ years experience", "Java expertise", "Team player"),  // Add more items
                List.of(new SkillDto("Java", "Expert", List.of("Spring Boot", "Hibernate")))
        );
    }
}