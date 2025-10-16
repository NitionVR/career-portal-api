package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobPostPermissionErrorHandlingTest extends BaseIntegrationTest {

    @Autowired
    private JobPostService jobPostService;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private TestHelper testHelper;

    private User hiringManager;
    private User recruiter;
    private JobPost jobPost;

    @BeforeEach
    void setUp() {
        hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);
        recruiter = testHelper.createUser("recruiter@test.com", Role.RECRUITER);
        recruiter.setOrganization(hiringManager.getOrganization());

        authenticateAs(hiringManager.getId());
        JobPostRequest request = createJobPostRequest();
        var response = jobPostService.createJobPost(request, hiringManager.getId());
        jobPost = jobPostRepository.findById(response.id()).orElseThrow();
    }

    @Test
    void creatingJobPostAsRecruiterGivesDescriptiveError() {
        authenticateAs(recruiter.getId());

        JobPostRequest request = createJobPostRequest();

        assertThatThrownBy(() -> jobPostService.createJobPost(request, recruiter.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only hiring managers can create job posts")
                .hasNoCause();
    }

    @Test
    void deletingJobPostAsRecruiterGivesDescriptiveError() {
        authenticateAs(recruiter.getId());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(jobPost.getId(), recruiter.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only hiring managers can delete job posts");
    }

    @Test
    void deletingOthersJobPostGivesDescriptiveError() {
        User otherHM = testHelper.createUser("other@test.com", Role.HIRING_MANAGER);
        otherHM.setOrganization(hiringManager.getOrganization());
        authenticateAs(otherHM.getId());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(jobPost.getId(), otherHM.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only delete your own job posts");
    }

    @Test
    void updatingJobPostFromDifferentOrganizationGivesDescriptiveError() {
        User otherRecruiter = testHelper.createUser("other-rec@test.com", Role.RECRUITER);
        authenticateAs(otherRecruiter.getId());

        JobPostRequest request = createJobPostRequest();

        assertThatThrownBy(() -> jobPostService.updateJobPost(jobPost.getId(), request, otherRecruiter.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void statusChangeOnDifferentOrganizationJobPostGivesDescriptiveError() {
        User otherRecruiter = testHelper.createUser("other-rec2@test.com", Role.RECRUITER);
        authenticateAs(otherRecruiter.getId());

        assertThatThrownBy(() -> jobPostService.publishJobPost(jobPost.getId(), otherRecruiter.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    private JobPostRequest createJobPostRequest() {
        return new JobPostRequest(
                "Software Engineer",
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
}