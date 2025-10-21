package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.ApplicationDetailsDto;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class JobApplicationServiceTest extends BaseIntegrationTest {

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JobApplicationAuditRepository jobApplicationAuditRepository;

    @Autowired
    private TestHelper testHelper;

    private User hiringManager;
    private User recruiter;
    private User candidate;
    private Organization organization;
    private JobPost jobPost;
    private JobApplication jobApplication;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();

        // Create organization and users
        hiringManager = testHelper.createUser("hm@test.com", Role.HIRING_MANAGER);
        organization = hiringManager.getOrganization();

        recruiter = testHelper.createUser("recruiter@test.com", Role.RECRUITER);
        recruiter.setOrganization(organization);
        recruiter = userRepository.save(recruiter);

        candidate = testHelper.createUser("candidate@test.com", Role.CANDIDATE);

        // Create job post
        authenticateAs(hiringManager.getId());
        jobPost = new JobPost();
        jobPost.setTitle("Test Job");
        jobPost.setCompany("Test Company");
        jobPost.setJobType("Full-time");
        jobPost.setDescription("Test Description");
        jobPost.setRemote("Remote");
        jobPost.setExperienceLevel("Senior");
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPost.setCreatedBy(hiringManager);
        jobPost.setOrganization(organization);
        jobPost = jobPostRepository.save(jobPost);

        // Create job application
        jobApplication = new JobApplication();
        jobApplication.setCandidate(candidate);
        jobApplication.setJobPost(jobPost);
        jobApplication.setStatus(JobApplicationStatus.APPLIED);
        jobApplication.setApplicationDate(LocalDateTime.now());
        jobApplication = jobApplicationRepository.save(jobApplication);

        // Create initial audit entry
        JobApplicationAudit initialAudit = new JobApplicationAudit(
                jobApplication,
                JobApplicationStatus.APPLIED,
                "Application submitted"
        );
        jobApplicationAuditRepository.save(initialAudit);
    }

    @Nested
    class ValidTransitions {

        @Test
        void transitionApplicationStatus_validTransition_shouldUpdateStatusAndAudit() {
            // Given
            authenticateAs(hiringManager.getId());
            JobApplicationStatus targetStatus = JobApplicationStatus.UNDER_REVIEW;
            long initialAuditCount = jobApplicationAuditRepository.count();

            // When
            ApplicationDetailsDto result = jobApplicationService.transitionApplicationStatus(
                    jobApplication.getId(),
                    targetStatus,
                    hiringManager.getId()
            );

            // Then
            assertThat(result.status()).isEqualTo(targetStatus.name());

            JobApplication updated = jobApplicationRepository.findById(jobApplication.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(targetStatus);

            long finalAuditCount = jobApplicationAuditRepository.count();
            assertThat(finalAuditCount).isEqualTo(initialAuditCount + 1);
        }

        @Test
        void transitionApplicationStatus_validTransitionToRejected_shouldUpdateStatusAndAudit() {
            // Given
            authenticateAs(hiringManager.getId());
            JobApplicationStatus targetStatus = JobApplicationStatus.REJECTED;
            long initialAuditCount = jobApplicationAuditRepository.count();

            // When
            ApplicationDetailsDto result = jobApplicationService.transitionApplicationStatus(
                    jobApplication.getId(),
                    targetStatus,
                    hiringManager.getId()
            );

            // Then
            assertThat(result.status()).isEqualTo(targetStatus.name());

            JobApplication updated = jobApplicationRepository.findById(jobApplication.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(targetStatus);

            long finalAuditCount = jobApplicationAuditRepository.count();
            assertThat(finalAuditCount).isEqualTo(initialAuditCount + 1);
        }

        @Test
        void recruiterCanTransitionStatus() {
            // Given
            authenticateAs(recruiter.getId());
            JobApplicationStatus targetStatus = JobApplicationStatus.UNDER_REVIEW;

            // When
            ApplicationDetailsDto result = jobApplicationService.transitionApplicationStatus(
                    jobApplication.getId(),
                    targetStatus,
                    recruiter.getId()
            );

            // Then
            assertThat(result.status()).isEqualTo(targetStatus.name());

            JobApplication updated = jobApplicationRepository.findById(jobApplication.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(targetStatus);
        }
    }

    @Nested
    class InvalidTransitions {

        @Test
        void transitionApplicationStatus_invalidApplicationId_shouldThrowResourceNotFoundException() {
            // Given
            authenticateAs(hiringManager.getId());
            UUID invalidApplicationId = UUID.randomUUID();

            // When & Then
            assertThatThrownBy(() ->
                    jobApplicationService.transitionApplicationStatus(
                            invalidApplicationId,
                            JobApplicationStatus.UNDER_REVIEW,
                            hiringManager.getId()
                    ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Job application not found");
        }

        @Test
        void transitionApplicationStatus_unauthorizedUserRole_shouldThrowUnauthorizedException() {
            // Given
            authenticateAs(candidate.getId());

            // When & Then
            assertThatThrownBy(() ->
                    jobApplicationService.transitionApplicationStatus(
                            jobApplication.getId(),
                            JobApplicationStatus.UNDER_REVIEW,
                            candidate.getId()
                    ))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Only Hiring Managers and Recruiters can transition application status");
        }

        @Test
        void transitionApplicationStatus_userNotInSameOrganization_shouldThrowUnauthorizedException() {
            // Given
            User otherHiringManager = testHelper.createUser("other-hm@test.com", Role.HIRING_MANAGER);
            authenticateAs(otherHiringManager.getId());

            // When & Then
            assertThatThrownBy(() ->
                    jobApplicationService.transitionApplicationStatus(
                            jobApplication.getId(),
                            JobApplicationStatus.UNDER_REVIEW,
                            otherHiringManager.getId()
                    ))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("not authorized to transition this application");
        }

        @Test
        void transitionApplicationStatus_invalidTransition_shouldThrowBadRequestException() {
            // Given
            authenticateAs(hiringManager.getId());

            // First, transition to HIRED (a terminal state) - this will be the current status
            jobApplication.setStatus(JobApplicationStatus.HIRED);
            jobApplication = jobApplicationRepository.save(jobApplication);

            JobApplicationStatus targetStatus = JobApplicationStatus.UNDER_REVIEW;

            // When & Then
            assertThatThrownBy(() ->
                    jobApplicationService.transitionApplicationStatus(
                            jobApplication.getId(),
                            targetStatus,
                            hiringManager.getId()
                    ))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid application status transition");
        }

        @Test
        void transitionApplicationStatus_sameStatus_shouldThrowBadRequestException() {
            // Given
            authenticateAs(hiringManager.getId());
            JobApplicationStatus currentStatus = jobApplication.getStatus(); // APPLIED

            // When & Then
            assertThatThrownBy(() ->
                    jobApplicationService.transitionApplicationStatus(
                            jobApplication.getId(),
                            currentStatus,
                            hiringManager.getId()
                    ))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Invalid application status transition");
        }
    }

    @Nested
    class MultipleTransitions {

        @Test
        void canTransitionThroughMultipleValidStates() {
            // Given
            authenticateAs(hiringManager.getId());

            // When & Then - APPLIED -> UNDER_REVIEW
            ApplicationDetailsDto result1 = jobApplicationService.transitionApplicationStatus(
                    jobApplication.getId(),
                    JobApplicationStatus.UNDER_REVIEW,
                    hiringManager.getId()
            );
            assertThat(result1.status()).isEqualTo(JobApplicationStatus.UNDER_REVIEW.name());

            // UNDER_REVIEW -> INTERVIEW_SCHEDULED
            ApplicationDetailsDto result2 = jobApplicationService.transitionApplicationStatus(
                    jobApplication.getId(),
                    JobApplicationStatus.INTERVIEW_SCHEDULED,
                    hiringManager.getId()
            );
            assertThat(result2.status()).isEqualTo(JobApplicationStatus.INTERVIEW_SCHEDULED.name());

            // INTERVIEW_SCHEDULED -> OFFER_EXTENDED
            ApplicationDetailsDto result3 = jobApplicationService.transitionApplicationStatus(
                    jobApplication.getId(),
                    JobApplicationStatus.OFFER_EXTENDED,
                    hiringManager.getId()
            );
            assertThat(result3.status()).isEqualTo(JobApplicationStatus.OFFER_EXTENDED.name());

            // Verify audit trail has all transitions
            long auditCount = jobApplicationAuditRepository.findByJobApplicationId(jobApplication.getId()).size();
            assertThat(auditCount).isEqualTo(4); // Initial + 3 transitions
        }
    }
}