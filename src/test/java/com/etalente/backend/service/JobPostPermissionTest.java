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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobPostPermissionTest extends BaseIntegrationTest {

    @Autowired
    private JobPostService jobPostService;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private JobPostPermissionService permissionService;

    private User hiringManager;
    private User recruiter;
    private User candidate;
    private JobPost draftJobPost;
    private JobPost openJobPost;

    @BeforeEach
    void setUp() {
        hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);
        recruiter = testHelper.createUser("recruiter@test.com", Role.RECRUITER);
        recruiter.setOrganization(hiringManager.getOrganization());
        candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);

        authenticateAs(hiringManager.getId());
        JobPostRequest request = createJobPostRequest("Software Engineer", "Draft job");
        JobPostResponse response = jobPostService.createJobPost(request, hiringManager.getId());
        draftJobPost = jobPostRepository.findById(response.id()).orElseThrow();

        JobPostRequest openRequest = createJobPostRequest("Senior Developer", "Open job");
        JobPostResponse openResponse = jobPostService.createJobPost(openRequest, hiringManager.getId());
        openJobPost = jobPostRepository.findById(openResponse.id()).orElseThrow();
        openJobPost.setStatus(JobPostStatus.OPEN);
        openJobPost = jobPostRepository.save(openJobPost);
    }

    // === CREATE TESTS ===

    @Test
    void hiringManagerCanCreateJobPost() {
        authenticateAs(hiringManager.getId());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatCode(() -> jobPostService.createJobPost(request, hiringManager.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCannotCreateJobPost() {
        authenticateAs(recruiter.getId());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, recruiter.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can create job posts");
    }

    @Test
    void candidateCannotCreateJobPost() {
        authenticateAs(candidate.getId());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, candidate.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === UPDATE TESTS ===

    @Test
    void hiringManagerCanUpdateOwnJobPost() {
        authenticateAs(hiringManager.getId());

        JobPostRequest updateRequest = createJobPostRequest("Updated Title", "Updated description");

        assertThatCode(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, hiringManager.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCanUpdateJobPostInSameOrganization() {
        authenticateAs(recruiter.getId());

        JobPostRequest updateRequest = createJobPostRequest("Recruiter Update", "Updated by recruiter");

        assertThatCode(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, recruiter.getId()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Recruiter Update");
    }

    @Test
    void recruiterCannotUpdateJobPostFromDifferentOrganization() {
        User otherHM = testHelper.createUser("other-hm@test.com", Role.HIRING_MANAGER);

        authenticateAs(otherHM.getId());
        JobPostRequest request = createJobPostRequest("Other Company Job", "Description");
        JobPostResponse response = jobPostService.createJobPost(request, otherHM.getId());

        authenticateAs(recruiter.getId());
        JobPostRequest updateRequest = createJobPostRequest("Hacked", "Should not work");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                response.id(), updateRequest, recruiter.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void candidateCannotUpdateJobPost() {
        authenticateAs(candidate.getId());

        JobPostRequest updateRequest = createJobPostRequest("Hacked Title", "Hacked");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, candidate.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === DELETE TESTS ===

    @Test
    void hiringManagerCanDeleteOwnJobPost() {
        authenticateAs(hiringManager.getId());

        assertThatCode(() -> jobPostService.deleteJobPost(draftJobPost.getId(), hiringManager.getId()))
                .doesNotThrowAnyException();

        assertThat(jobPostRepository.findById(draftJobPost.getId())).isEmpty();
    }

    @Test
    void recruiterCannotDeleteJobPost() {
        authenticateAs(recruiter.getId());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), recruiter.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can delete job posts");
    }

    @Test
    void hiringManagerCannotDeleteOtherHiringManagerJobPost() {
        User otherHM = testHelper.createUser("other-hm@test.com", Role.HIRING_MANAGER);
        otherHM.setOrganization(hiringManager.getOrganization());

        authenticateAs(otherHM.getId());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), otherHM.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only delete your own job posts");
    }

    @Test
    void candidateCannotDeleteJobPost() {
        authenticateAs(candidate.getId());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), candidate.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === STATUS CHANGE TESTS ===

    @Test
    void hiringManagerCanPublishJobPost() {
        authenticateAs(hiringManager.getId());

        assertThatCode(() -> jobPostService.publishJobPost(draftJobPost.getId(), hiringManager.getId()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void recruiterCanPublishJobPost() {
        authenticateAs(recruiter.getId());

        assertThatCode(() -> jobPostService.publishJobPost(draftJobPost.getId(), recruiter.getId()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void hiringManagerCanCloseJobPost() {
        authenticateAs(hiringManager.getId());

        assertThatCode(() -> jobPostService.closeJobPost(openJobPost.getId(), null, hiringManager.getId()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(openJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.CLOSED);
    }

    @Test
    void recruiterCanCloseJobPost() {
        authenticateAs(recruiter.getId());

        assertThatCode(() -> jobPostService.closeJobPost(openJobPost.getId(), null, recruiter.getId()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(openJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.CLOSED);
    }

    @Test
    void candidateCannotChangeJobPostStatus() {
        authenticateAs(candidate.getId());

        assertThatThrownBy(() -> jobPostService.publishJobPost(draftJobPost.getId(), candidate.getId()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void recruiterCannotChangeStatusOfJobPostFromDifferentOrganization() {
        User otherHM = testHelper.createUser("other-hm2@test.com", Role.HIRING_MANAGER);

        authenticateAs(otherHM.getId());
        JobPostRequest request = createJobPostRequest("Other Company Job", "Description");
        JobPostResponse response = jobPostService.createJobPost(request, otherHM.getId());

        authenticateAs(recruiter.getId());

        assertThatThrownBy(() -> jobPostService.publishJobPost(response.id(), recruiter.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    // === VIEW PERMISSION TESTS ===

    @Test
    void hiringManagerCanViewOwnDraftJobPost() {
        authenticateAs(hiringManager.getId());

        assertThatCode(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCanViewDraftJobPostInSameOrganization() {
        authenticateAs(recruiter.getId());

        assertThatCode(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void candidateCannotViewDraftJobPost() {
        authenticateAs(candidate.getId());

        assertThatThrownBy(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void candidateCanViewOpenJobPost() {
        authenticateAs(candidate.getId());

        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void everyoneCanViewPublicJobPost() {
        authenticateAs(hiringManager.getId());
        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();

        authenticateAs(recruiter.getId());
        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();

        authenticateAs(candidate.getId());
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
        assertThat(permissionService.canView(hiringManager, openJobPost)).isTrue();
        assertThat(permissionService.canView(recruiter, openJobPost)).isTrue();
        assertThat(permissionService.canView(candidate, openJobPost)).isTrue();

        assertThat(permissionService.canView(hiringManager, draftJobPost)).isTrue();
        assertThat(permissionService.canView(recruiter, draftJobPost)).isTrue();
        assertThat(permissionService.canView(candidate, draftJobPost)).isFalse();
    }

    // Helper methods
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
}