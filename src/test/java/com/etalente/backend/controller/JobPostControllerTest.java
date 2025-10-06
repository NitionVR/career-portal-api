package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class JobPostControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private Faker faker;
    private Organization organization;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        organization = new Organization();
        organization.setName(faker.company().name());
        organizationRepository.save(organization);
    }

    @Test
    void createJobPost_shouldCreateJobPost_whenHiringManager() throws Exception {
        // Given
        User user = createUser(Role.HIRING_MANAGER);
        String token = generateToken(user);
        JobPostRequest request = createFakeJobPostRequest();

        // When & Then
        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is(request.title())))
                .andExpect(jsonPath("$.status", is("DRAFT")));

        assertEquals(1, jobPostRepository.count());
    }

    @Test
    void createJobPost_shouldFail_whenNotHiringManager() throws Exception {
        // Given
        User user = createUser(Role.CANDIDATE);
        String token = generateToken(user);
        JobPostRequest request = createFakeJobPostRequest();

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
        User user = createUser(Role.HIRING_MANAGER);
        String token = generateToken(user);

        JobPostRequest request = new JobPostRequest(
                "", // Invalid: empty title
                faker.company().name(),
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
        User user = createUser(Role.HIRING_MANAGER);
        String token = generateToken(user);
        JobPost jobPost = createJobPost(user);

        // When & Then
        mockMvc.perform(get("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + token))
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
        User user = createUser(Role.HIRING_MANAGER);
        String token = generateToken(user);
        createJobPost(user);
        createJobPost(user);
        createJobPost(user);

        // When & Then
        mockMvc.perform(get("/api/job-posts")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.totalElements", is(3)));
    }

    @Test
    void listMyJobPosts_shouldReturnOnlyUsersPosts() throws Exception {
        // Given
        User user1 = createUser(Role.HIRING_MANAGER);
        User user2 = createUser(Role.HIRING_MANAGER);
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
        User user = createUser(Role.HIRING_MANAGER);
        String token = generateToken(user);
        JobPost jobPost = createJobPost(user);
        JobPostRequest updateRequest = createFakeJobPostRequest();

        // When & Then
        mockMvc.perform(put("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(updateRequest.title())))
                .andExpect(jsonPath("$.jobType", is(updateRequest.jobType())));
    }

    @Test
    void updateJobPost_shouldFail_whenNotOwner() throws Exception {
        // Given
        User owner = createUser(Role.HIRING_MANAGER);
        // Create a user from a DIFFERENT organization
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg = organizationRepository.save(otherOrg);

        User other = new User();
        other.setEmail(faker.internet().emailAddress());
        other.setRole(Role.HIRING_MANAGER);
        other.setOrganization(otherOrg);  // Different organization
        other = userRepository.save(other);

        String token = generateToken(other);
        JobPost jobPost = createJobPost(owner);
        JobPostRequest updateRequest = createFakeJobPostRequest();

        // When & Then - Now it should fail because different organization
        mockMvc.perform(put("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteJobPost_shouldDeletePost_whenOwner() throws Exception {
        // Given
        User user = createUser(Role.HIRING_MANAGER);
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
        User owner = createUser(Role.HIRING_MANAGER);
        User other = createUser(Role.HIRING_MANAGER);
        String token = generateToken(other);
        JobPost jobPost = createJobPost(owner);

        // When & Then
        mockMvc.perform(delete("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertEquals(1, jobPostRepository.count());
    }

    // Helper methods
    private User createUser(Role role) {
        User user = new User();
        user.setEmail(faker.internet().emailAddress());
        user.setRole(role);
        user.setFirstName(faker.name().firstName());
        user.setLastName(faker.name().lastName());
        user.setOrganization(organization);
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
        jobPost.setTitle(faker.lorem().characters(50));
        jobPost.setCompany(faker.company().name());
        jobPost.setJobType(faker.options().option("Full-time", "Part-time", "Contract"));
        jobPost.setDescription(faker.lorem().characters(250));
        jobPost.setRemote(faker.options().option("On-site", "Remote", "Hybrid"));
        jobPost.setExperienceLevel(faker.options().option("Entry-level", "Mid-level", "Senior-level"));
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost.setCreatedBy(user);
        jobPost.setOrganization(organization);
        jobPost.setDatePosted(
                DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault())
                        .format(faker.date().past(30, TimeUnit.DAYS).toInstant())
        );
        return jobPostRepository.save(jobPost);
    }

    private JobPostRequest createFakeJobPostRequest() {
        return new JobPostRequest(
                faker.lorem().characters(50), // Shorter title
                faker.company().name(),
                faker.options().option("Full-time", "Part-time", "Contract"),
                faker.lorem().characters(250), // Shorter description
                new LocationDto(
                        faker.address().streetAddress(),
                        faker.address().zipCode(),
                        faker.address().city(),
                        faker.address().countryCode(),
                        faker.address().stateAbbr()
                ),
                faker.options().option("On-site", "Remote", "Hybrid"),
                String.format("%d-%d", faker.number().numberBetween(80000, 100000), faker.number().numberBetween(101000, 150000)),
                faker.options().option("Entry-level", "Mid-level", "Senior-level"),
                generateFakeList(3, () -> faker.lorem().sentence(5)),
                generateFakeList(3, () -> faker.lorem().sentence(5)),
                Collections.emptyList()
        );
    }

    private <T> List<T> generateFakeList(int count, java.util.function.Supplier<T> supplier) {
        return IntStream.range(0, count).mapToObj(i -> supplier.get()).collect(Collectors.toList());
    }
}