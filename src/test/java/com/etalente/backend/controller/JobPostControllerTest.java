package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.dto.SkillDto;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.service.JobPostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private ObjectMapper objectMapper;

    @Autowired
    private JobPostService jobPostService;

    @Autowired
    private TestHelper testHelper;

    private Faker faker;
    private Organization organization;
    private User hiringManager;
    private String hiringManagerJwt;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();
        hiringManager = testHelper.createUser("hm@example.com", Role.HIRING_MANAGER);
        hiringManagerJwt = testHelper.generateJwtForUser(hiringManager);
        faker = new Faker();
        organization = new Organization();
        organization.setName(faker.company().name());
        organizationRepository.save(organization);
    }

    @Test
    void createJobPost_shouldCreateJobPost_whenHiringManager() throws Exception {
        // Given
        JobPostRequest request = createFakeJobPostRequest();

        // When & Then
        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + hiringManagerJwt)
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
        String token = testHelper.createUserAndGetJwt("candidate@example.com", Role.CANDIDATE);
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
        JobPostRequest request = new JobPostRequest(
                "", // Invalid: empty title
                faker.company().name(),
                "Full-time",
                "Short", // Invalid: too short description
                null,
                "On-site",
                null,
                "Entry-level",
                Collections.emptyList(), // Invalid: empty responsibilities
                Collections.emptyList(), // Invalid: empty qualifications
                null
        );

        // When & Then
        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + hiringManagerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getJobPost_shouldReturnJobPost_whenExists() throws Exception {
        // Given
        User owner = testHelper.createUser("owner@example.com", Role.HIRING_MANAGER);
        JobPost jobPost = createJobPost(owner);
        String ownerToken = testHelper.generateJwtForUser(owner);

        mockMvc.perform(get("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
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
        createAndPublishJobPost(hiringManager);
        createAndPublishJobPost(hiringManager);
        createAndPublishJobPost(hiringManager);

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
        User user1 = testHelper.createUser("user1-list@example.com", Role.HIRING_MANAGER);
        User user2 = testHelper.createUser("user2@example.com", Role.HIRING_MANAGER);
        String token = testHelper.generateJwtForUser(user1);

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
        User user = testHelper.createUser("owner-update@example.com", Role.HIRING_MANAGER);
        String token = testHelper.generateJwtForUser(user);
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
        User owner = testHelper.createUser("owner@example.com", Role.HIRING_MANAGER);
        String otherUserToken = testHelper.createUserAndGetJwt("other@example.com", Role.HIRING_MANAGER);
        JobPost jobPost = createJobPost(owner);
        JobPostRequest updateRequest = createFakeJobPostRequest();

        // When & Then
        mockMvc.perform(put("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteJobPost_shouldDeletePost_whenOwner() throws Exception {
        User user = testHelper.createUser("owner-delete@example.com", Role.HIRING_MANAGER);
        String token = testHelper.generateJwtForUser(user);
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
        User owner = testHelper.createUser("owner@example.com", Role.HIRING_MANAGER);
        String otherUserToken = testHelper.createUserAndGetJwt("other@example.com", Role.HIRING_MANAGER);
        JobPost jobPost = createJobPost(owner);

        // When & Then
        mockMvc.perform(delete("/api/job-posts/{id}", jobPost.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        assertEquals(1, jobPostRepository.count());
    }

    // Helper methods
    private JobPost createJobPost(User user) {
        JobPost jobPost = new JobPost();
        jobPost.setTitle(faker.lorem().characters(50));
        jobPost.setCompany(faker.company().name());
        jobPost.setJobType(faker.options().option("Full-time", "Part-time", "Contract"));
        jobPost.setDescription(faker.lorem().characters(250));
        jobPost.setRemote(faker.options().option("On-site", "Remote", "Hybrid"));
        jobPost.setExperienceLevel(faker.options().option("Entry-level", "Mid-level", "Senior-level"));
        ObjectNode location = objectMapper.createObjectNode();
        location.put("city", faker.address().city());
        location.put("countryCode", faker.address().countryCode());
        jobPost.setLocation(location);
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost.setCreatedBy(user);
        jobPost.setOrganization(user.getOrganization());
        jobPost.setDatePosted(
                DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault())
                        .format(faker.date().past(30, TimeUnit.DAYS).toInstant())
        );
        return jobPostRepository.save(jobPost);
    }

    private void createAndPublishJobPost(User user) {
        JobPost jobPost = createJobPost(user);
        authenticateAs(user.getId());
        jobPostService.publishJobPost(jobPost.getId(), user.getId());
    }

    private JobPostRequest createFakeJobPostRequest() {
        return new JobPostRequest(
                faker.lorem().characters(50),
                faker.company().name(),
                "Full-time",
                faker.lorem().characters(250),
                new LocationDto(
                        faker.address().streetAddress(),
                        faker.address().zipCode(),
                        faker.address().city(),
                        faker.address().countryCode(),
                        faker.address().stateAbbr()
                ),
                "On-site",
                String.format("%d-%d", faker.number().numberBetween(80000, 100000), faker.number().numberBetween(101000, 150000)),
                "Entry-level",
                generateFakeList(3, () -> faker.lorem().sentence(5)),
                generateFakeList(3, () -> faker.lorem().sentence(5)),
                Collections.emptyList()
        );
    }

    private <T> List<T> generateFakeList(int count, java.util.function.Supplier<T> supplier) {
        return IntStream.range(0, count).mapToObj(i -> supplier.get()).collect(Collectors.toList());
    }

    @Test
    void createJobPost_withLowercaseExperienceLevel_shouldSucceed() throws Exception {
        JobPostRequest request = new JobPostRequest(
                "Test Title",
                "Test Company",
                "Full-time",
                "A valid description that is at least fifty characters long.",
                new LocationDto("Test City", "USA", "12345", "CA", "123 Main St"),
                "Remote",
                "$100,000",
                "entry-level",
                List.of("Responsibility 1"),
                List.of("Qualification 1"),
                List.of(new SkillDto("Java", "Expert", List.of("Spring")))
        );

        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + hiringManagerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.experienceLevel").value("Entry-level"));
    }
}
