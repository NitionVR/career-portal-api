package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobPostMultiTenancyTest extends BaseIntegrationTest {

    @Autowired
    private JobPostService jobPostService;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private TestHelper testHelper;

    private User hiringManagerA;
    private User hiringManagerB;
    private User recruiterA;
    private User candidate;
    private JobPost jobPostOrgA;
    private JobPost jobPostOrgB;

    @BeforeEach
    void setUp() {
        // Create Users and Organizations
        hiringManagerA = testHelper.createUser("hm-a@companya.com", Role.HIRING_MANAGER);
        hiringManagerB = testHelper.createUser("hm-b@companyb.com", Role.HIRING_MANAGER);
        recruiterA = testHelper.createUser("recruiter-a@companya.com", Role.RECRUITER);
        candidate = testHelper.createUser("candidate@example.com", Role.CANDIDATE);

        // Create job post for Organization A
        authenticateAs(hiringManagerA.getId().toString());
        JobPostRequest requestA = createJobPostRequest("Senior Developer at Company A", "OPEN");
        JobPostResponse responseA = jobPostService.createJobPost(requestA, hiringManagerA.getId());
        jobPostOrgA = jobPostRepository.findById(responseA.id()).orElseThrow();
        jobPostOrgA.setStatus(JobPostStatus.OPEN); // Make it public
        jobPostOrgA = jobPostRepository.save(jobPostOrgA);

        // Create job post for Organization B
        authenticateAs(hiringManagerB.getId().toString());
        JobPostRequest requestB = createJobPostRequest("Data Analyst at Company B", "DRAFT");
        JobPostResponse responseB = jobPostService.createJobPost(requestB, hiringManagerB.getId());
        jobPostOrgB = jobPostRepository.findById(responseB.id()).orElseThrow();
    }

    @Test
    void hiringManagerCanOnlySeeOwnOrganizationJobPosts() {
        authenticateAs(hiringManagerA.getId().toString());

        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, null, null);

        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).title()).contains("Company A");
        assertThat(jobs.getContent().get(0).title()).doesNotContain("Company B");
    }

    @Test
    void hiringManagerCannotAccessOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getId().toString());

        // Try to access job post from Organization B
        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void hiringManagerCannotUpdateOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getId().toString());

        JobPostRequest updateRequest = createJobPostRequest("Hacked Title", "OPEN");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                jobPostOrgB.getId(), updateRequest, hiringManagerA.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void hiringManagerCannotDeleteOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getId().toString());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(
                jobPostOrgB.getId(), hiringManagerA.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void recruiterCanSeeJobPostsFromSameOrganization() {
        authenticateAs(recruiterA.getId().toString());
        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, null, null);

        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).title()).contains("Company A");
    }

    @Test
    void recruiterCannotAccessOtherOrganizationJobPost() {
        authenticateAs(recruiterA.getId().toString());

        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void candidateCanOnlySeePublicJobPosts() {
        authenticateAs(candidate.getId().toString());

        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, null, null);

        // Should only see OPEN job posts
        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).id()).isEqualTo(jobPostOrgA.getId());
        assertThat(jobs.getContent().get(0).status()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void candidateCanAccessPublicJobPost() {
        authenticateAs(candidate.getId().toString());

        // Can access OPEN job post
        JobPostResponse response = jobPostService.getJobPost(jobPostOrgA.getId());
        assertThat(response.title()).contains("Company A");
    }

    @Test
    void candidateCannotAccessDraftJobPost() {
        authenticateAs(candidate.getId().toString());

        // Cannot access DRAFT job post
        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void listJobPostsByUserRespectsOrganizationBoundaries() {
        authenticateAs(hiringManagerA.getId().toString());

        // Can list own organization user's posts
        Page<JobPostResponse> ownPosts = jobPostService.listJobPostsByUser(
                hiringManagerA.getId(), PageRequest.of(0, 10));
        assertThat(ownPosts.getContent()).hasSize(1);

        // Cannot list other organization user's posts
        assertThatThrownBy(() -> jobPostService.listJobPostsByUser(
                hiringManagerB.getId(), PageRequest.of(0, 10)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("can only view job posts from users in your organization");
    }

    @Test
    void creatingJobPostAutomaticallyAssignsOrganization() {
        authenticateAs(hiringManagerA.getId().toString());

        JobPostRequest request = createJobPostRequest("New Job", "DRAFT");
        JobPostResponse response = jobPostService.createJobPost(request, hiringManagerA.getId());

        JobPost savedJobPost = jobPostRepository.findById(response.id()).orElseThrow();
        assertThat(savedJobPost.getOrganization()).isNotNull();
        assertThat(savedJobPost.getOrganization().getId()).isEqualTo(hiringManagerA.getOrganization().getId());
    }

    @Test
    void candidateCannotCreateJobPost() {
        authenticateAs(candidate.getId().toString());

        JobPostRequest request = createJobPostRequest("Candidate Job", "DRAFT");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, candidate.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("must belong to an organization");
    }

    @Test
    void publicJobSearchDoesNotLeakDraftPosts() {
        // Create multiple job posts with different statuses
        authenticateAs(hiringManagerA.getId().toString());

        JobPostRequest draftRequest = createJobPostRequest("Draft Job A", "DRAFT");
        jobPostService.createJobPost(draftRequest, hiringManagerA.getId());

        JobPostRequest openRequest = createJobPostRequest("Open Job A", "OPEN");
        JobPostResponse openResponse = jobPostService.createJobPost(openRequest, hiringManagerA.getId());
        JobPost openJob = jobPostRepository.findById(openResponse.id()).orElseThrow();
        openJob.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(openJob);

        // Candidate searches
        authenticateAs(candidate.getId().toString());
        Page<JobPostResponse> results = jobPostService.listJobPosts(PageRequest.of(0, 10), null, null, null, null, null);

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
                hiringManagerA.getOrganization(), PageRequest.of(0, 10));
        Page<JobPost> orgBJobs = jobPostRepository.findByOrganization(
                hiringManagerB.getOrganization(), PageRequest.of(0, 10));

        assertThat(orgAJobs.getContent()).hasSize(1);
        assertThat(orgBJobs.getContent()).hasSize(1);
        assertThat(orgAJobs.getContent().get(0).getId()).isNotEqualTo(
                orgBJobs.getContent().get(0).getId());
    }

    // Helper methods
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

    private void authenticateAs(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, null)
        );
    }
}
