package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.ApplicantSummaryDto;
import com.etalente.backend.model.*;
import com.etalente.backend.model.JobApplicationStatus;
import com.etalente.backend.repository.JobApplicationRepository;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
public class ApplicantControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Faker faker;

    private User hiringManager1;
    private User recruiter1;
    private User candidate1;
    private User candidate2;
    private Organization org1;
    private JobPost jobPost1;
    private JobPost jobPost2;
    private JobApplication application1;
    private JobApplication application2;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        testHelper.cleanupDatabase();

        // Setup Organization and Users
        hiringManager1 = createUser("hm1@org1.com", "hm1", Role.HIRING_MANAGER, null);
        org1 = createOrganization("Org1 Inc.", hiringManager1);
        hiringManager1.setOrganization(org1);
        userRepository.save(hiringManager1);

        recruiter1 = createUser("rec1@org1.com", "rec1", Role.RECRUITER, org1);
        userRepository.save(recruiter1);

        candidate1 = createUser("cand1@email.com", "cand1", Role.CANDIDATE, null);
        candidate1.setFirstName("Alice");
        candidate1.setLastName("Smith");
        candidate1.setProfileImageUrl("http://example.com/alice.jpg");
        ObjectNode profile1 = objectMapper.createObjectNode();
        profile1.put("experienceYears", 5);
        profile1.put("education", "BSc Computer Science");
        candidate1.setProfile(profile1);
        userRepository.save(candidate1);

        candidate2 = createUser("cand2@email.com", "cand2", Role.CANDIDATE, null);
        candidate2.setFirstName("Bob");
        candidate2.setLastName("Johnson");
        candidate2.setProfileImageUrl("http://example.com/bob.jpg");
        ObjectNode profile2 = objectMapper.createObjectNode();
        profile2.put("experienceYears", 2);
        profile2.put("education", "MSc Data Science");
        candidate2.setProfile(profile2);
        userRepository.save(candidate2);

        // Setup JobPosts
        jobPost1 = createJobPost(hiringManager1, "Software Engineer", "Org1 Inc.", "Full-time", "Remote", "Mid-level", JobPostStatus.OPEN.name(), List.of("Java", "Spring"), "London");
        jobPost2 = createJobPost(hiringManager1, "Data Scientist", "Org1 Inc.", "Contract", "Hybrid", "Senior", JobPostStatus.OPEN.name(), List.of("Python", "ML"), "New York");

        // Setup JobApplications
        application1 = createJobApplication(jobPost1, candidate1, JobApplicationStatus.APPLIED);
        application2 = createJobApplication(jobPost2, candidate2, JobApplicationStatus.UNDER_REVIEW);
    }

    // --- Helper Methods ---
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

    private Organization createOrganization(String name, User createdBy) {
        Organization org = new Organization();
        org.setName(name);
        org.setIndustry("IT");
        org.setCreatedBy(createdBy);
        return organizationRepository.save(org);
    }

    private JobPost createJobPost(User createdBy, String title, String company, String jobType, String remote, String experienceLevel, String status, List<String> skills, String location) {
        JobPost jobPost = new JobPost();
        jobPost.setTitle(title);
        jobPost.setCompany(company);
        jobPost.setJobType(jobType);
        jobPost.setDescription(title + " description");
        jobPost.setRemote(remote);
        jobPost.setExperienceLevel(experienceLevel);
        jobPost.setStatus(JobPostStatus.valueOf(status));
        jobPost.setCreatedBy(createdBy);
        jobPost.setOrganization(createdBy.getOrganization());
        jobPost.setDatePosted(DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault()).format(faker.date().past(30, TimeUnit.DAYS).toInstant()));

        ObjectNode locationNode = objectMapper.createObjectNode();
        locationNode.put("city", location);
        jobPost.setLocation(locationNode);

        jobPost.setSkills(objectMapper.valueToTree(skills.stream().map(s -> {
            ObjectNode skillNode = objectMapper.createObjectNode();
            skillNode.put("name", s);
            skillNode.put("level", "Intermediate");
            return skillNode;
        }).collect(Collectors.toList())));

        return jobPostRepository.save(jobPost);
    }

    private JobApplication createJobApplication(JobPost jobPost, User candidate, JobApplicationStatus status) {
        JobApplication application = new JobApplication();
        application.setJobPost(jobPost);
        application.setCandidate(candidate);
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

    // --- Tests ---

    @Test
    @DisplayName("Hiring Manager should get a paginated list of applicants for their organization")
    void getApplicants_hiringManager_shouldReturnPaginatedApplicants() throws Exception {
        authenticateAsWithRoles(hiringManager1);

        mockMvc.perform(get("/api/applicants")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].candidateName", anyOf(is("Alice Smith"), is("Bob Johnson"))))
                .andExpect(jsonPath("$.content[0].jobTitle", anyOf(is("Software Engineer"), is("Data Scientist"))));
    }

    @Test
    @DisplayName("Recruiter should get a paginated list of applicants for their organization")
    void getApplicants_recruiter_shouldReturnPaginatedApplicants() throws Exception {
        authenticateAsWithRoles(recruiter1);

        mockMvc.perform(get("/api/applicants")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].candidateName", anyOf(is("Alice Smith"), is("Bob Johnson"))))
                .andExpect(jsonPath("$.content[0].jobTitle", anyOf(is("Software Engineer"), is("Data Scientist"))));
    }

    @Test
    @DisplayName("Candidate should be forbidden from accessing applicants endpoint")
    void getApplicants_candidate_shouldBeForbidden() throws Exception {
        authenticateAsWithRoles(candidate1);

        mockMvc.perform(get("/api/applicants")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthorized user should be unauthorized from accessing applicants endpoint")
    void getApplicants_unauthorized_shouldBeUnauthorized() throws Exception {
        mockMvc.perform(get("/api/applicants")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should filter applicants by jobId")
    void getApplicants_shouldFilterByJobId() throws Exception {
        authenticateAsWithRoles(hiringManager1);

        mockMvc.perform(get("/api/applicants")
                        .param("jobId", jobPost1.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].jobId", is(jobPost1.getId().toString())))
                .andExpect(jsonPath("$.content[0].candidateName", is("Alice Smith")));
    }

    @Test
    @DisplayName("Should filter applicants by status")
    void getApplicants_shouldFilterByStatus() throws Exception {
        authenticateAsWithRoles(hiringManager1);

        mockMvc.perform(get("/api/applicants")
                        .param("statuses", JobApplicationStatus.APPLIED.name())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is(JobApplicationStatus.APPLIED.name())))
                .andExpect(jsonPath("$.content[0].candidateName", is("Alice Smith")));
    }

    @Test
    @DisplayName("Should filter applicants by search term (candidate name or job title)")
    void getApplicants_shouldFilterBySearchTerm() throws Exception {
        authenticateAsWithRoles(hiringManager1);

        mockMvc.perform(get("/api/applicants")
                        .param("search", "Alice")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].candidateName", is("Alice Smith")));

        mockMvc.perform(get("/api/applicants")
                        .param("search", "Software")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].jobTitle", is("Software Engineer")));
    }

    @Test
    @DisplayName("Should filter applicants by skill search")
    void getApplicants_shouldFilterBySkillSearch() throws Exception {
        authenticateAsWithRoles(hiringManager1);

        mockMvc.perform(get("/api/applicants")
                        .param("skillSearch", "Java")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].candidateName", is("Alice Smith")));
    }

    @Test
    @DisplayName("Should filter applicants by minimum experience years")
    void getApplicants_shouldFilterByExperienceMin() throws Exception {
        authenticateAsWithRoles(hiringManager1);

        mockMvc.perform(get("/api/applicants")
                        .param("experienceMin", "4")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].candidateName", is("Alice Smith")));
    }

    @Test
    @DisplayName("Should filter applicants by education")
    void getApplicants_shouldFilterByEducation() throws Exception {
        authenticateAsWithRoles(hiringManager1);

        mockMvc.perform(get("/api/applicants")
                        .param("education", "Computer Science")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].candidateName", is("Alice Smith")));
    }

    @Test
    @DisplayName("Should filter applicants by location")
    void getApplicants_shouldFilterByLocation() throws Exception {
        authenticateAsWithRoles(hiringManager1);

        mockMvc.perform(get("/api/applicants")
                        .param("location", "London")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].candidateName", is("Alice Smith")));
    }

    // @Test
    // @DisplayName("Should filter applicants by AI match score minimum")
    // void getApplicants_shouldFilterByAiMatchScoreMin() throws Exception {
    //     authenticateAsWithRoles(hiringManager1);

    //     // Assuming AI match score is 0 for now, so filtering by 0 should return all
    //     mockMvc.perform(get("/api/applicants")
    //                     .param("aiMatchScoreMin", "0")
    //                     .contentType(MediaType.APPLICATION_JSON))
    //             .andExpect(status().isOk())
    //             .andExpect(jsonPath("$.content", hasSize(2)));
    // }
}
