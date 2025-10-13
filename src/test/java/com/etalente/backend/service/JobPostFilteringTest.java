package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class JobPostFilteringTest extends BaseIntegrationTest {

    @Autowired
    private JobPostService jobPostService;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Faker faker;

    private User hmUser1;
    private User hmUser2;
    private User candidateUser;
    private Organization org1;
    private Organization org2;

    private JobPost job1_org1_open_java;
    private JobPost job2_org1_draft_python;
    private JobPost job3_org2_open_react;
    private JobPost job4_org2_closed_node;

    @BeforeEach
    void setUp() {
        faker = new Faker(); // Initialize Faker here
        SecurityContextHolder.clearContext(); // Clear context before each test
        // Clear database before each test
        jobPostRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        org1 = createOrganization("Tech Solutions Inc.");
        org2 = createOrganization("Finance Innovations Ltd.");

        hmUser1 = createUser("hm1@tech.com", "hm1", Role.HIRING_MANAGER, org1);
        hmUser2 = createUser("hm2@finance.com", "hm2", Role.HIRING_MANAGER, org2);
        candidateUser = createUser("candidate@test.com", "candidate", Role.CANDIDATE, null);

        // Job Posts for Org1
        job1_org1_open_java = createJobPost(
                hmUser1,
                "Senior Java Developer",
                "Tech Solutions Inc.",
                "Full-time",
                "Remote",
                "Senior",
                JobPostStatus.OPEN,
                List.of("Java", "Spring Boot"),
                List.of("Backend Development")
        );

        job2_org1_draft_python = createJobPost(
                hmUser1,
                "Junior Python Engineer",
                "Tech Solutions Inc.",
                "On-site",
                "Remote",
                "Entry-level",
                JobPostStatus.DRAFT,
                List.of("Python", "Django"),
                List.of("Web Development")
        );

        // Job Posts for Org2
        job3_org2_open_react = createJobPost(
                hmUser2,
                "React Frontend Developer",
                "Finance Innovations Ltd.",
                "Contract",
                "Hybrid",
                "Mid-level",
                JobPostStatus.OPEN,
                List.of("React", "TypeScript"),
                List.of("Frontend Development")
        );

        job4_org2_closed_node = createJobPost(
                hmUser2,
                "Node.js Backend Engineer",
                "Finance Innovations Ltd.",
                "Full-time",
                "Remote",
                "Senior",
                JobPostStatus.CLOSED,
                List.of("Node.js", "Express"),
                List.of("Backend Development")
        );
    }

    // --- Helper Methods ---
    private Organization createOrganization(String name) {
        Organization org = new Organization();
        org.setName(name);
        org.setIndustry("IT");
        return organizationRepository.save(org);
    }

    private User createUser(String email, String username, Role role, Organization organization) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setOrganization(organization);
        user.setEmailVerified(true);
        user.setProfileComplete(true);
        return userRepository.save(user);
    }

    private JobPost createJobPost(
            User createdBy,
            String title,
            String company,
            String jobType,
            String remote,
            String experienceLevel,
            JobPostStatus status,
            List<String> skills,
            List<String> responsibilities) {

        JobPost jobPost = new JobPost();
        jobPost.setTitle(title);
        jobPost.setCompany(company);
        jobPost.setJobType(jobType);
        jobPost.setDescription(title + " description");
        jobPost.setRemote(remote);
        jobPost.setExperienceLevel(experienceLevel);
        jobPost.setStatus(status);
        jobPost.setCreatedBy(createdBy);
        jobPost.setOrganization(createdBy.getOrganization());
        jobPost.setDatePosted(
                DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault())
                        .format(faker.date().past(30, TimeUnit.DAYS).toInstant())
        );

        // Set location
        ObjectNode locationNode = objectMapper.createObjectNode();
        locationNode.put("city", "Test City");
        locationNode.put("countryCode", "TC");
        jobPost.setLocation(locationNode);

        // Set skills (simplified for test)
        jobPost.setSkills(objectMapper.valueToTree(skills.stream().map(s -> {
            ObjectNode skillNode = objectMapper.createObjectNode();
            skillNode.put("name", s);
            skillNode.put("level", "Intermediate");
            return skillNode;
        }).collect(Collectors.toList())));

        // Set responsibilities (simplified for test)
        jobPost.setResponsibilities(objectMapper.valueToTree(responsibilities));

        return jobPostRepository.save(jobPost);
    }

    // --- Tests for public (unauthenticated) access ---

    @Test
    void unauthenticatedUserCanOnlySeeOpenJobs() {
        SecurityContextHolder.clearContext(); // Ensure unauthenticated
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, null, null);

        assertThat(jobs.getTotalElements()).isEqualTo(2); // job1_org1_open_java, job3_org2_open_react
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactlyInAnyOrder("Senior Java Developer", "React Frontend Developer");
    }

    @Test
    void unauthenticatedUserFiltersBySearchTerm() {
        SecurityContextHolder.clearContext();
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), "java", null, null, null, null);

        assertThat(jobs.getTotalElements()).isEqualTo(1);
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactly("Senior Java Developer");
    }

    @Test
    void unauthenticatedUserFiltersBySkillSearch() {
        SecurityContextHolder.clearContext();
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, "react", null, null, null);

        assertThat(jobs.getTotalElements()).isEqualTo(1);
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactly("React Frontend Developer");
    }

    @Test
    void unauthenticatedUserFiltersByExperienceLevels() {
        SecurityContextHolder.clearContext();
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, List.of("Senior"), null, null);

        assertThat(jobs.getTotalElements()).isEqualTo(1);
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactly("Senior Java Developer");
    }

    @Test
    void unauthenticatedUserFiltersByJobTypes() {
        SecurityContextHolder.clearContext();
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, List.of("Contract"), null);

        assertThat(jobs.getTotalElements()).isEqualTo(1);
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactly("React Frontend Developer");
    }

    @Test
    void unauthenticatedUserFiltersByWorkTypes() {
        SecurityContextHolder.clearContext();
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, null, List.of("Remote"));

        assertThat(jobs.getTotalElements()).isEqualTo(1);
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactly("Senior Java Developer");
    }

    // --- Tests for authenticated (Hiring Manager) access ---

    @Test
    void hiringManagerCanSeeAllOwnOrganizationJobs() {
        authenticateAs(hmUser1.getEmail());
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, null, null);

        assertThat(jobs.getTotalElements()).isEqualTo(2); // job1_org1_open_java, job2_org1_draft_python
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactlyInAnyOrder("Senior Java Developer", "Junior Python Engineer");
    }

    @Test
    void hiringManagerFiltersOwnOrganizationJobsBySearchTerm() {
        authenticateAs(hmUser1.getEmail());
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), "java", null, null, null, null);

        assertThat(jobs.getTotalElements()).isEqualTo(1);
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactly("Senior Java Developer");
    }

    @Test
    void hiringManagerFiltersOwnOrganizationJobsBySkillSearch() {
        authenticateAs(hmUser1.getEmail());
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, "spring", null, null, null);

        assertThat(jobs.getTotalElements()).isEqualTo(1);
        assertThat(jobs.getContent()).extracting(JobPostResponse::title)
                .containsExactly("Senior Java Developer");
    }

    @Test
    void hiringManagerCannotSeeOtherOrganizationJobs() {
        authenticateAs(hmUser1.getEmail());
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, null, null);

        assertThat(jobs.getTotalElements()).isEqualTo(2);
        assertThat(jobs.getContent()).extracting(JobPostResponse::company)
                .containsOnly(org1.getName());
    }
}