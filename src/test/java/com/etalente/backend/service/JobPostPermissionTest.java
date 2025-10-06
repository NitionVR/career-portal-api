package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JobPostPermissionTest extends BaseIntegrationTest {

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

    @Autowired
    private JobPostPermissionService permissionService;

    private Organization organization;
    private User hiringManager;
    private User recruiter;
    private User candidate;
    private JobPost draftJobPost;
    private JobPost openJobPost;

    @BeforeEach
    void setUp() {
        // Create organization
        organization = new Organization();
        organization.setName("Test Company");
        organization.setIndustry("Technology");
        organization = organizationRepository.save(organization);

        // Create hiring manager
        hiringManager = createUser("hm@test.com", "hm1", Role.HIRING_MANAGER, organization);

        // Create recruiter
        recruiter = createUser("recruiter@test.com", "rec1", Role.RECRUITER, organization);

        // Create candidate
        candidate = createUser("candidate@test.com", "candidate1", Role.CANDIDATE, null);

        // Create draft job post by hiring manager
        authenticateAs(hiringManager.getEmail());
        JobPostRequest request = createJobPostRequest("Software Engineer", "Draft job");
        JobPostResponse response = jobPostService.createJobPost(request, hiringManager.getEmail());
        draftJobPost = jobPostRepository.findById(response.id()).orElseThrow();

        // Create open job post
        JobPostRequest openRequest = createJobPostRequest("Senior Developer", "Open job");
        JobPostResponse openResponse = jobPostService.createJobPost(openRequest, hiringManager.getEmail());
        openJobPost = jobPostRepository.findById(openResponse.id()).orElseThrow();
        openJobPost.setStatus(JobPostStatus.OPEN);
        openJobPost = jobPostRepository.save(openJobPost);
    }

    // === CREATE TESTS ===

    @Test
    void hiringManagerCanCreateJobPost() {
        authenticateAs(hiringManager.getEmail());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatCode(() -> jobPostService.createJobPost(request, hiringManager.getEmail()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCannotCreateJobPost() {
        authenticateAs(recruiter.getEmail());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can create job posts");
    }

    @Test
    void candidateCannotCreateJobPost() {
        authenticateAs(candidate.getEmail());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === UPDATE TESTS ===

    @Test
    void hiringManagerCanUpdateOwnJobPost() {
        authenticateAs(hiringManager.getEmail());

        JobPostRequest updateRequest = createJobPostRequest("Updated Title", "Updated description");

        assertThatCode(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, hiringManager.getEmail()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCanUpdateJobPostInSameOrganization() {
        authenticateAs(recruiter.getEmail());

        JobPostRequest updateRequest = createJobPostRequest("Recruiter Update", "Updated by recruiter");

        assertThatCode(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, recruiter.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Recruiter Update");
    }

    @Test
    void recruiterCannotUpdateJobPostFromDifferentOrganization() {
        // Create another organization with a job post
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg.setIndustry("Finance");
        otherOrg = organizationRepository.save(otherOrg);

        User otherHM = createUser("other-hm@test.com", "otherhm", Role.HIRING_MANAGER, otherOrg);

        authenticateAs(otherHM.getEmail());
        JobPostRequest request = createJobPostRequest("Other Company Job", "Description");
        JobPostResponse response = jobPostService.createJobPost(request, otherHM.getEmail());

        // Try to update as recruiter from first organization
        authenticateAs(recruiter.getEmail());
        JobPostRequest updateRequest = createJobPostRequest("Hacked", "Should not work");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                response.id(), updateRequest, recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void candidateCannotUpdateJobPost() {
        authenticateAs(candidate.getEmail());

        JobPostRequest updateRequest = createJobPostRequest("Hacked Title", "Hacked");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === DELETE TESTS ===

    @Test
    void hiringManagerCanDeleteOwnJobPost() {
        authenticateAs(hiringManager.getEmail());

        assertThatCode(() -> jobPostService.deleteJobPost(draftJobPost.getId(), hiringManager.getEmail()))
                .doesNotThrowAnyException();

        assertThat(jobPostRepository.findById(draftJobPost.getId())).isEmpty();
    }

    @Test
    void recruiterCannotDeleteJobPost() {
        authenticateAs(recruiter.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can delete job posts");
    }

    @Test
    void hiringManagerCannotDeleteOtherHiringManagerJobPost() {
        // Create another hiring manager in same organization
        User otherHM = createUser("other-hm@test.com", "otherhm", Role.HIRING_MANAGER, organization);

        authenticateAs(otherHM.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), otherHM.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only delete your own job posts");
    }

    @Test
    void candidateCannotDeleteJobPost() {
        authenticateAs(candidate.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === STATUS CHANGE TESTS ===

    @Test
    void hiringManagerCanPublishJobPost() {
        authenticateAs(hiringManager.getEmail());

        assertThatCode(() -> jobPostService.publishJobPost(draftJobPost.getId(), hiringManager.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void recruiterCanPublishJobPost() {
        authenticateAs(recruiter.getEmail());

        assertThatCode(() -> jobPostService.publishJobPost(draftJobPost.getId(), recruiter.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void hiringManagerCanCloseJobPost() {
        authenticateAs(hiringManager.getEmail());

        assertThatCode(() -> jobPostService.closeJobPost(openJobPost.getId(), hiringManager.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(openJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.CLOSED);
    }

    @Test
    void recruiterCanCloseJobPost() {
        authenticateAs(recruiter.getEmail());

        assertThatCode(() -> jobPostService.closeJobPost(openJobPost.getId(), recruiter.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(openJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.CLOSED);
    }

    @Test
    void candidateCannotChangeJobPostStatus() {
        authenticateAs(candidate.getEmail());

        assertThatThrownBy(() -> jobPostService.publishJobPost(draftJobPost.getId(), candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void recruiterCannotChangeStatusOfJobPostFromDifferentOrganization() {
        // Create another organization with a job post
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg.setIndustry("Finance");
        otherOrg = organizationRepository.save(otherOrg);

        User otherHM = createUser("other-hm2@test.com", "otherhm2", Role.HIRING_MANAGER, otherOrg);

        authenticateAs(otherHM.getEmail());
        JobPostRequest request = createJobPostRequest("Other Company Job", "Description");
        JobPostResponse response = jobPostService.createJobPost(request, otherHM.getEmail());

        // Try to publish as recruiter from first organization
        authenticateAs(recruiter.getEmail());

        assertThatThrownBy(() -> jobPostService.publishJobPost(response.id(), recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    // === VIEW PERMISSION TESTS ===

    @Test
    void hiringManagerCanViewOwnDraftJobPost() {
        authenticateAs(hiringManager.getEmail());

        assertThatCode(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCanViewDraftJobPostInSameOrganization() {
        authenticateAs(recruiter.getEmail());

        assertThatCode(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void candidateCannotViewDraftJobPost() {
        authenticateAs(candidate.getEmail());

        assertThatThrownBy(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void candidateCanViewOpenJobPost() {
        authenticateAs(candidate.getEmail());

        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void everyoneCanViewPublicJobPost() {
        // Hiring Manager
        authenticateAs(hiringManager.getEmail());
        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();

        // Recruiter
        authenticateAs(recruiter.getEmail());
        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();

        // Candidate
        authenticateAs(candidate.getEmail());
        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();
    }

    // === PERMISSION SERVICE UNIT TESTS ===

    @Test
    void permissionServiceVerifyCanCreate() {
        assertThatCode(() -> permissionService.verifyCanCreate(hiringManager))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> permissionService.verifyCanCreate(recruiter))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can create job posts");

        assertThatThrownBy(() -> permissionService.verifyCanCreate(candidate))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void permissionServiceVerifyCanUpdate() {
        assertThatCode(() -> permissionService.verifyCanUpdate(hiringManager, draftJobPost))
                .doesNotThrowAnyException();

        assertThatCode(() -> permissionService.verifyCanUpdate(recruiter, draftJobPost))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> permissionService.verifyCanUpdate(candidate, draftJobPost))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void permissionServiceVerifyCanDelete() {
        assertThatCode(() -> permissionService.verifyCanDelete(hiringManager, draftJobPost))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> permissionService.verifyCanDelete(recruiter, draftJobPost))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can delete job posts");
    }

    @Test
    void permissionServiceVerifyCanChangeStatus() {
        assertThatCode(() -> permissionService.verifyCanChangeStatus(hiringManager, draftJobPost))
                .doesNotThrowAnyException();

        assertThatCode(() -> permissionService.verifyCanChangeStatus(recruiter, draftJobPost))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> permissionService.verifyCanChangeStatus(candidate, draftJobPost))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void permissionServiceCanView() {
        // Public job post - everyone can view
        assertThat(permissionService.canView(hiringManager, openJobPost)).isTrue();
        assertThat(permissionService.canView(recruiter, openJobPost)).isTrue();
        assertThat(permissionService.canView(candidate, openJobPost)).isTrue();

        // Draft job post - only organization members can view
        assertThat(permissionService.canView(hiringManager, draftJobPost)).isTrue();
        assertThat(permissionService.canView(recruiter, draftJobPost)).isTrue();
        assertThat(permissionService.canView(candidate, draftJobPost)).isFalse();
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

    private JobPostRequest createJobPostRequest(String title, String description) {
        return new JobPostRequest(
                title,
                "Test Company",
                "Full-time",
                description,
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
