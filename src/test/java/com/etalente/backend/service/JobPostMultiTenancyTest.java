package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JobPostMultiTenancyTest extends BaseIntegrationTest {

    @Autowired
    private JobPostService jobPostService;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private Organization orgA;
    private Organization orgB;
    private User hiringManagerA;
    private User hiringManagerB;
    private User recruiterA;
    private User candidate;
    private JobPost jobPostOrgA;
    private JobPost jobPostOrgB;

    @BeforeEach
    void setUp() {
        // Create Organization A
        orgA = new Organization();
        orgA.setName("Company A");
        orgA.setIndustry("Technology");
        orgA = organizationRepository.save(orgA);

        // Create Organization B
        orgB = new Organization();
        orgB.setName("Company B");
        orgB.setIndustry("Finance");
        orgB = organizationRepository.save(orgB);

        // Create Hiring Manager for Org A
        hiringManagerA = createUser("hm-a@companya.com", "hmA", Role.HIRING_MANAGER, orgA);

        // Create Hiring Manager for Org B
        hiringManagerB = createUser("hm-b@companyb.com", "hmB", Role.HIRING_MANAGER, orgB);

        // Create Recruiter for Org A
        recruiterA = createUser("recruiter-a@companya.com", "recruiterA", Role.RECRUITER, orgA);

        // Create Candidate (no organization)
        candidate = createUser("candidate@example.com", "candidate1", Role.CANDIDATE, null);

        // Create job post for Organization A
        authenticateAs(hiringManagerA.getEmail());
        JobPostRequest requestA = createJobPostRequest("Senior Developer at Company A", "OPEN");
        JobPostResponse responseA = jobPostService.createJobPost(requestA, hiringManagerA.getEmail());
        jobPostOrgA = jobPostRepository.findById(responseA.id()).orElseThrow();
        jobPostOrgA.setStatus(JobPostStatus.OPEN); // Make it public
        jobPostOrgA = jobPostRepository.save(jobPostOrgA);

        // Create job post for Organization B
        authenticateAs(hiringManagerB.getEmail());
        JobPostRequest requestB = createJobPostRequest("Data Analyst at Company B", "DRAFT");
        JobPostResponse responseB = jobPostService.createJobPost(requestB, hiringManagerB.getEmail());
        jobPostOrgB = jobPostRepository.findById(responseB.id()).orElseThrow();
    }

    @Test
    void hiringManagerCanOnlySeeOwnOrganizationJobPosts() {
        authenticateAs(hiringManagerA.getEmail());

        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10));

        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).title()).contains("Company A");
        assertThat(jobs.getContent().get(0).title()).doesNotContain("Company B");
    }

    @Test
    void hiringManagerCannotAccessOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getEmail());

        // Try to access job post from Organization B
        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void hiringManagerCannotUpdateOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getEmail());

        JobPostRequest updateRequest = createJobPostRequest("Hacked Title", "OPEN");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                jobPostOrgB.getId(), updateRequest, hiringManagerA.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void hiringManagerCannotDeleteOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(
                jobPostOrgB.getId(), hiringManagerA.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void recruiterCanSeeJobPostsFromSameOrganization() {
        authenticateAs(recruiterA.getEmail());

        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10));

        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).title()).contains("Company A");
    }

    @Test
    void recruiterCannotAccessOtherOrganizationJobPost() {
        authenticateAs(recruiterA.getEmail());

        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void candidateCanOnlySeePublicJobPosts() {
        authenticateAs(candidate.getEmail());

        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10));

        // Should only see OPEN job posts
        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).id()).isEqualTo(jobPostOrgA.getId());
        assertThat(jobs.getContent().get(0).status()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void candidateCanAccessPublicJobPost() {
        authenticateAs(candidate.getEmail());

        // Can access OPEN job post
        JobPostResponse response = jobPostService.getJobPost(jobPostOrgA.getId());
        assertThat(response.title()).contains("Company A");
    }

    @Test
    void candidateCannotAccessDraftJobPost() {
        authenticateAs(candidate.getEmail());

        // Cannot access DRAFT job post
        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void listJobPostsByUserRespectsOrganizationBoundaries() {
        authenticateAs(hiringManagerA.getEmail());

        // Can list own organization user's posts
        Page<JobPostResponse> ownPosts = jobPostService.listJobPostsByUser(
                hiringManagerA.getEmail(), PageRequest.of(0, 10));
        assertThat(ownPosts.getContent()).hasSize(1);

        // Cannot list other organization user's posts
        assertThatThrownBy(() -> jobPostService.listJobPostsByUser(
                hiringManagerB.getEmail(), PageRequest.of(0, 10)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("can only view job posts from users in your organization");
    }

    @Test
    void creatingJobPostAutomaticallyAssignsOrganization() {
        authenticateAs(hiringManagerA.getEmail());

        JobPostRequest request = createJobPostRequest("New Job", "DRAFT");
        JobPostResponse response = jobPostService.createJobPost(request, hiringManagerA.getEmail());

        JobPost savedJobPost = jobPostRepository.findById(response.id()).orElseThrow();
        assertThat(savedJobPost.getOrganization()).isNotNull();
        assertThat(savedJobPost.getOrganization().getId()).isEqualTo(orgA.getId());
    }

    @Test
    void candidateCannotCreateJobPost() {
        authenticateAs(candidate.getEmail());

        JobPostRequest request = createJobPostRequest("Candidate Job", "DRAFT");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("must belong to an organization");
    }

    @Test
    void publicJobSearchDoesNotLeakDraftPosts() {
        // Create multiple job posts with different statuses
        authenticateAs(hiringManagerA.getEmail());

        JobPostRequest draftRequest = createJobPostRequest("Draft Job A", "DRAFT");
        jobPostService.createJobPost(draftRequest, hiringManagerA.getEmail());

        JobPostRequest openRequest = createJobPostRequest("Open Job A", "OPEN");
        JobPostResponse openResponse = jobPostService.createJobPost(openRequest, hiringManagerA.getEmail());
        JobPost openJob = jobPostRepository.findById(openResponse.id()).orElseThrow();
        openJob.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(openJob);

        // Candidate searches
        authenticateAs(candidate.getEmail());
        Page<JobPostResponse> results = jobPostService.listJobPosts(PageRequest.of(0, 10));

        // Should only see OPEN jobs
        assertThat(results.getContent())
                .extracting(JobPostResponse::status)
                .containsOnly(JobPostStatus.OPEN);

        assertThat(results.getContent())
                .extracting(JobPostResponse::title)
                .noneMatch(title -> title.contains("Draft"));
    }

    @Test
    void organizationDataIsolationInRepository() {
        // Direct repository test
        Page<JobPost> orgAJobs = jobPostRepository.findByOrganization(
                orgA, PageRequest.of(0, 10));
        Page<JobPost> orgBJobs = jobPostRepository.findByOrganization(
                orgB, PageRequest.of(0, 10));

        assertThat(orgAJobs.getContent()).hasSize(1);
        assertThat(orgBJobs.getContent()).hasSize(1);
        assertThat(orgAJobs.getContent().get(0).getId()).isNotEqualTo(
                orgBJobs.getContent().get(0).getId());
    }

    // Helper methods
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

    private JobPostRequest createJobPostRequest(String title, String status) {
        return new JobPostRequest(
                title,
                "Test Company",
                "Full-time",
                "Test description",
                new LocationDto("123 Main St", "12345", "San Francisco", "US", "CA"),
                "Hybrid",
                "$100k-$150k",
                "Senior",
                List.of("Develop features", "Code reviews"),
                List.of("5+ years experience", "Java expertise"),
                List.of()
        );
    }

    private void authenticateAs(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}