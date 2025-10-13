package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
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

class JobPostPermissionErrorHandlingTest extends BaseIntegrationTest {

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

    private Organization organization;
    private User hiringManager;
    private User recruiter;
    private JobPost jobPost;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setName("Test Company");
        organization.setIndustry("Technology");
        organization = organizationRepository.save(organization);

        hiringManager = createUser("hm@test.com", "hm1", Role.HIRING_MANAGER, organization);
        recruiter = createUser("recruiter@test.com", "rec1", Role.RECRUITER, organization);

        authenticateAs(hiringManager.getEmail());
        JobPostRequest request = createJobPostRequest();
        var response = jobPostService.createJobPost(request, hiringManager.getEmail());
        jobPost = jobPostRepository.findById(response.id()).orElseThrow();
    }

    @Test
    void creatingJobPostAsRecruiterGivesDescriptiveError() {
        authenticateAs(recruiter.getEmail());

        JobPostRequest request = createJobPostRequest();

        assertThatThrownBy(() -> jobPostService.createJobPost(request, recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only hiring managers can create job posts")
                .hasNoCause();
    }

    @Test
    void deletingJobPostAsRecruiterGivesDescriptiveError() {
        authenticateAs(recruiter.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(jobPost.getId(), recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only hiring managers can delete job posts");
    }

    @Test
    void deletingOthersJobPostGivesDescriptiveError() {
        User otherHM = createUser("other@test.com", "other", Role.HIRING_MANAGER, organization);
        authenticateAs(otherHM.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(jobPost.getId(), otherHM.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only delete your own job posts");
    }

    @Test
    void updatingJobPostFromDifferentOrganizationGivesDescriptiveError() {
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg.setIndustry("Finance");
        otherOrg = organizationRepository.save(otherOrg);

        User otherRecruiter = createUser("other-rec@test.com", "otherRec", Role.RECRUITER, otherOrg);
        authenticateAs(otherRecruiter.getEmail());

        JobPostRequest request = createJobPostRequest();

        assertThatThrownBy(() -> jobPostService.updateJobPost(jobPost.getId(), request, otherRecruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void statusChangeOnDifferentOrganizationJobPostGivesDescriptiveError() {
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg = organizationRepository.save(otherOrg);

        User otherRecruiter = createUser("other-rec2@test.com", "otherRec2", Role.RECRUITER, otherOrg);
        authenticateAs(otherRecruiter.getEmail());

        assertThatThrownBy(() -> jobPostService.publishJobPost(jobPost.getId(), otherRecruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    private User createUser(String email, String username, Role role, Organization org) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setOrganization(org);
        user.setEmailVerified(true);
        user.setProfileComplete(true);
        return userRepository.save(user);
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