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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

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
        organization.setName("Test Company");
        organization.setIndustry("Tech");
        organization = organizationRepository.save(organization);

        // Create users
        hiringManager = createUser("hm@test.com", "hm1", Role.HIRING_MANAGER, organization);
        recruiter = createUser("recruiter@test.com", "rec1", Role.RECRUITER, organization);
        candidate = createUser("candidate@test.com", "cand1", Role.CANDIDATE, null);

        // Generate tokens
        hmToken = generateToken(hiringManager);
        recruiterToken = generateToken(recruiter);
        candidateToken = generateToken(candidate);

        // Create job post
        jobPost = new JobPost();
        jobPost.setTitle("Test Job");
        jobPost.setDescription("This is a test description for a job post. It needs to be at least 50 characters long to pass validation.");
        jobPost.setCreatedBy(hiringManager);
        jobPost.setOrganization(organization);
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost = jobPostRepository.save(jobPost);
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

    private String generateToken(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("")
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();

        return jwtService.generateToken(Map.of("role", user.getRole().name()), userDetails);
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
