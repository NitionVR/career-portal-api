package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.dto.SkillDto;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class JobPostPermissionControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private TestHelper testHelper;

    private JobPost jobPost;

    @BeforeEach
    void setUp() {
        User hiringManager = testHelper.createUser("hm-" + UUID.randomUUID() + "@test.com", Role.HIRING_MANAGER);

        jobPost = new JobPost();
        jobPost.setTitle("Test Job");
        jobPost.setDescription("This is a test description for a job post. It needs to be at least 50 characters long to pass validation.");
        jobPost.setJobType("Full-time");
        jobPost.setExperienceLevel("Mid-level");
        jobPost.setCompany(hiringManager.getCompanyName());

        ObjectNode location = objectMapper.createObjectNode();
        location.put("city", "San Francisco");
        location.put("countryCode", "US");
        jobPost.setLocation(location);

        jobPost.setCreatedBy(hiringManager);
        jobPost.setOrganization(hiringManager.getOrganization());
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost = jobPostRepository.save(jobPost);
    }

    // === CREATE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanCreateJobPost() throws Exception {
        String hmToken = testHelper.createUserAndGetJwt("hm-" + UUID.randomUUID() + "@test.com", Role.HIRING_MANAGER);
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
        String recruiterToken = testHelper.createUserAndGetJwt("recruiter-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCannotCreateJobPost() throws Exception {
        String candidateToken = testHelper.createUserAndGetJwt("candidate-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);
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
        String hmToken = testHelper.createUserAndGetJwt(jobPost.getCreatedBy().getEmail(), jobPost.getCreatedBy().getRole());
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void recruiterCanUpdateJobPost() throws Exception {
        String recruiterToken = testHelper.createUserAndGetJwt("recruiter-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void candidateCannotUpdateJobPost() throws Exception {
        String candidateToken = testHelper.createUserAndGetJwt("candidate-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);
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
        String hmToken = testHelper.createUserAndGetJwt(jobPost.getCreatedBy().getEmail(), jobPost.getCreatedBy().getRole());
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void recruiterCannotDeleteJobPost() throws Exception {
        String recruiterToken = testHelper.createUserAndGetJwt("recruiter-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCannotDeleteJobPost() throws Exception {
        String candidateToken = testHelper.createUserAndGetJwt("candidate-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    // === STATUS CHANGE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanPublishJobPost() throws Exception {
        String hmToken = testHelper.createUserAndGetJwt(jobPost.getCreatedBy().getEmail(), jobPost.getCreatedBy().getRole());
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void recruiterCanPublishJobPost() throws Exception {
        String recruiterToken = testHelper.createUserAndGetJwt("recruiter-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void candidateCannotPublishJobPost() throws Exception {
        String candidateToken = testHelper.createUserAndGetJwt("candidate-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hiringManagerCanCloseJobPost() throws Exception {
        String hmToken = testHelper.createUserAndGetJwt(jobPost.getCreatedBy().getEmail(), jobPost.getCreatedBy().getRole());
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
        String recruiterToken = testHelper.createUserAndGetJwt("recruiter-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
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
        String candidateToken = testHelper.createUserAndGetJwt("candidate-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/close")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hiringManagerCanUpdateJobPostStatus() throws Exception {
        String hmToken = testHelper.createUserAndGetJwt(jobPost.getCreatedBy().getEmail(), jobPost.getCreatedBy().getRole());
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/status")
                        .header("Authorization", "Bearer " + hmToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void recruiterCanUpdateJobPostStatus() throws Exception {
        String recruiterToken = testHelper.createUserAndGetJwt("recruiter-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/status")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    // === LISTING ENDPOINT TESTS ===

    @Test
    void recruiterCanAccessMyPostsEndpoint() throws Exception {
        String recruiterToken = testHelper.createUserAndGetJwt("recruiter-" + UUID.randomUUID() + "@test.com", Role.RECRUITER);
        mockMvc.perform(get("/api/job-posts/my-posts")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk());
    }

    @Test
    void candidateCannotAccessMyPostsEndpoint() throws Exception {
        String candidateToken = testHelper.createUserAndGetJwt("candidate-" + UUID.randomUUID() + "@test.com", Role.CANDIDATE);
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
    private JobPostRequest createJobPostRequest() {
        return new JobPostRequest(
                "Software Engineer",
                "Test Company",
                "Full-time",
                "This is a test description for a job post. It needs to be at least 50 characters long to pass validation.",
                new LocationDto("123 Main St", "12345", "San Francisco", "US", "CA"),
                "Hybrid",
                "$100k-$150k",
                "Senior-level",
                List.of("Develop features", "Code reviews", "Mentor juniors"),
                List.of("5+ years experience", "Java expertise", "Team player"),
                List.of(new SkillDto("Java", "Expert", List.of("Spring Boot", "Hibernate")))
        );
    }
}
