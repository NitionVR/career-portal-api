package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.is;

@Transactional
class JobPostControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createJobPost_shouldCreateJobPost_whenHiringManager() throws Exception {
        // Given
        User user = createUser("hiring.manager@example.com", Role.HIRING_MANAGER);
        String token = generateToken(user);

        JobPostRequest request = new JobPostRequest(
                "Software Engineer",
                "Acme Corp",
                "Full-time",
                "We are looking for a talented software engineer to join our team. You will be responsible for developing high-quality applications.",
                new LocationDto("123 Main St", "10001", "New York", "US", "NY"),
                "Hybrid",
                "100000-120000",
                "Mid-level",
                Arrays.asList("Develop software", "Write tests", "Review code"),
                Arrays.asList("BS in Computer Science", "3+ years experience"),
                Collections.emptyList()
        );

        // When & Then
        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Software Engineer")))
                .andExpect(jsonPath("$.status", is("DRAFT")));

        assertEquals(1, jobPostRepository.count());
    }

    @Test
    void createJobPost_shouldFail_whenNotHiringManager() throws Exception {
        // Given
        User user = createUser("candidate@example.com", Role.CANDIDATE);
        String token = generateToken(user);

        JobPostRequest request = new JobPostRequest(
                "Software Engineer",
                "Acme Corp",
                "Full-time",
                "We are looking for a talented software engineer to join our team.",
                null,
                "Remote",
                null,
                "Mid-level",
                Arrays.asList("Develop software"),
                Arrays.asList("BS in Computer Science"),
                null
        );

        // When & Then
        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createJobPost_shouldFail_whenInvalidData() throws Exception {
        // Given
        User user = createUser("hiring.manager@example.com", Role.HIRING_MANAGER);
        String token = generateToken(user);

        JobPostRequest request = new JobPostRequest(
                "", // Invalid: empty title
                "Acme Corp",
                "Invalid-Type", // Invalid job type
                "Short", // Invalid: too short description
                null,
                "Invalid-Remote", // Invalid remote type
                null,
                "Invalid-Level", // Invalid experience level
                Collections.emptyList(), // Invalid: empty responsibilities
                Collections.emptyList(), // Invalid: empty qualifications
                null
        );

        // When & Then
        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message.title").exists())
                .andExpect(jsonPath("$.message.jobType").exists())
                .andExpect(jsonPath("$.message.description").exists());
    }

    @Test
    void getJobPost_shouldReturnJobPost_whenExists() throws Exception {
        // Given
        User user = createUser("hiring.manager@example.com", Role.HIRING_MANAGER);
        JobPost jobPost = createJobPost(user);

        // When & Then
        mockMvc.perform(get("/api/job-posts/{id}", jobPost.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(jobPost.getId().toString())))
                .andExpect(jsonPath("$.title", is(jobPost.getTitle())));
    }

    @Test
    void getJobPost_shouldReturn404_whenNotExists() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/job-posts/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listJobPosts_shouldReturnPaginatedResults() throws Exception {
        // Given
        User user = createUser("hiring.manager@example.com", Role.HIRING_MANAGER);
        createJobPost(user);
        createJobPost(user);
        createJobPost(user);

        // When & Then
        mockMvc.perform(get("/api/job-posts")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    void listMyJobPosts_shouldReturnOnlyUsersPosts() throws Exception {
        // Given
        User user1 = createUser("hiring1@example.com", Role.HIRING_MANAGER);
        User user2 = createUser("hiring2@example.com", Role.HIRING_MANAGER);
        String token = generateToken(user1);

        createJobPost(user1);
        createJobPost(user1);
        createJobPost(user2);

        // When & Then
        mockMvc.perform(get("/api/job-posts/my-posts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void updateJobPost_shouldUpdatePost_whenOwner() throws Exception {
        // Given
        User user = createUser("hiring.manager@example.com", Role.HIRING_MANAGER);
        String token = generateToken(user);
        JobPost jobPost = createJobPost(user);

        JobPostRequest updateRequest = new JobPostRequest(
                "Updated Software Engineer",
                "Updated Corp",
                "Part-time",
                "Updated description that is long enough to meet the validation requirements for this field",
                new LocationDto("456 Updated St", "20002", "Washington", "US", "DC"),
                "Remote",
                "120000-140000",
                "Senior-level",
                Arrays.asList("Lead development", "Mentor juniors"),
                Arrays.asList("MS in Computer Science", "5+ years experience"),
                Collections.emptyList()
        );

        // When & Then
        mockMvc.perform(put("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Software Engineer")))
                .andExpect(jsonPath("$.jobType", is("Part-time")));
    }

    @Test
    void updateJobPost_shouldFail_whenNotOwner() throws Exception {
        // Given
        User owner = createUser("owner@example.com", Role.HIRING_MANAGER);
        User other = createUser("other@example.com", Role.HIRING_MANAGER);
        String token = generateToken(other);
        JobPost jobPost = createJobPost(owner);

        JobPostRequest updateRequest = new JobPostRequest(
                "Hacked Title",
                "Hacked Corp",
                "Full-time",
                "This is an attempt to update someone else's job post which should not be allowed",
                null,
                "Remote",
                null,
                "Mid-level",
                Arrays.asList("Hack the system"),
                Arrays.asList("Be a hacker"),
                null
        );

        // When & Then
        mockMvc.perform(put("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteJobPost_shouldDeletePost_whenOwner() throws Exception {
        // Given
        User user = createUser("hiring.manager@example.com", Role.HIRING_MANAGER);
        String token = generateToken(user);
        JobPost jobPost = createJobPost(user);

        // When & Then
        mockMvc.perform(delete("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertEquals(0, jobPostRepository.count());
    }

    @Test
    void deleteJobPost_shouldFail_whenNotOwner() throws Exception {
        // Given
        User owner = createUser("owner@example.com", Role.HIRING_MANAGER);
        User other = createUser("other@example.com", Role.HIRING_MANAGER);
        String token = generateToken(other);
        JobPost jobPost = createJobPost(owner);

        // When & Then
        mockMvc.perform(delete("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertEquals(1, jobPostRepository.count());
    }

    // Helper methods
    private User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        return userRepository.save(user);
    }

    private String generateToken(User user) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                "",
                Collections.singletonList(authority)
        );
        return jwtService.generateToken(userDetails);
    }

    private JobPost createJobPost(User user) {
        JobPost jobPost = new JobPost();
        jobPost.setTitle("Test Software Engineer");
        jobPost.setCompany("Test Corp");
        jobPost.setJobType("Full-time");
        jobPost.setDescription("Test description");
        jobPost.setRemote("Hybrid");
        jobPost.setExperienceLevel("Mid-level");
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost.setCreatedBy(user);
        jobPost.setDatePosted("2024-01-01");
        return jobPostRepository.save(jobPost);
    }
}