package com.etalente.backend.service.impl;

import com.etalente.backend.dto.*;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.*;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.JobApplicationService;
import com.etalente.backend.integration.novu.NovuWorkflowService;
import com.etalente.backend.service.JobPostPermissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class JobApplicationServiceImpl implements JobApplicationService {

    private static final Logger log = LoggerFactory.getLogger(JobApplicationServiceImpl.class);

    private final JobApplicationRepository jobApplicationRepository;
    private final JobPostRepository jobPostRepository;
    private final OrganizationContext organizationContext;
    private final JobApplicationAuditRepository jobApplicationAuditRepository;
    private final NovuWorkflowService novuWorkflowService;
    private final JobPostPermissionService permissionService;
    private final UserRepository userRepository;

    public JobApplicationServiceImpl(JobApplicationRepository jobApplicationRepository,
                                     JobPostRepository jobPostRepository,
                                     OrganizationContext organizationContext,
                                     JobApplicationAuditRepository jobApplicationAuditRepository,
                                     NovuWorkflowService novuWorkflowService,
                                     JobPostPermissionService permissionService,
                                     UserRepository userRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobPostRepository = jobPostRepository;
        this.organizationContext = organizationContext;
        this.jobApplicationAuditRepository = jobApplicationAuditRepository;
        this.novuWorkflowService = novuWorkflowService;
        this.permissionService = permissionService;
        this.userRepository = userRepository;
    }

    @Override
    public Page<ApplicationSummaryDto> getMyApplications(Pageable pageable, String search, String sort) {
        User currentUser = organizationContext.getCurrentUser();
        return jobApplicationRepository.findAll(JobApplicationSpecification.withFilters(currentUser.getId(), search, sort), pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public ApplicationDetailsDto getApplicationDetails(UUID applicationId) {
        log.info("Attempting to retrieve application details for ID: {}", applicationId);
        User currentUser = userRepository.findById(organizationContext.getCurrentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Scenario 1: Candidate viewing their own application
        if (application.getCandidate().getId().equals(currentUser.getId())) {
            return toDetailsDto(application);
        }

        // Scenario 2: Hiring Manager or Recruiter viewing applications for their organization's job posts
        JobPost jobPost = application.getJobPost();
        if (currentUser.getRole() == Role.HIRING_MANAGER || currentUser.getRole() == Role.RECRUITER) {
            if (permissionService.belongsToSameOrganization(currentUser, jobPost)) {
                return toDetailsDto(application);
            }
        }

        throw new UnauthorizedException("You are not authorized to view this application.");
    }

    @Override
    public ApplicationSummaryDto applyForJob(UUID jobPostId) {
        User currentUser = organizationContext.getCurrentUser();
        JobPost jobPost = jobPostRepository.findById(jobPostId)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found"));

        if (jobPost.getStatus() != JobPostStatus.OPEN) {
            throw new BadRequestException("Cannot apply for a job that is not OPEN.");
        }

        if (jobApplicationRepository.existsByCandidateIdAndJobPostId(currentUser.getId(), jobPostId)) {
            throw new BadRequestException("You have already applied for this job.");
        }

        JobApplication application = new JobApplication();
        application.setCandidate(currentUser);
        application.setJobPost(jobPost);
        application.setStatus(JobApplicationStatus.APPLIED);

        JobApplication savedApplication = jobApplicationRepository.save(application);

        jobApplicationAuditRepository.save(new JobApplicationAudit(savedApplication, JobApplicationStatus.APPLIED, "Application submitted."));

        // Construct WorkflowTriggerRequest
        WorkflowTriggerRequest workflowRequest = new WorkflowTriggerRequest();
        workflowRequest.setSubscriberId(jobPost.getCreatedBy().getId().toString());
        workflowRequest.setEmail(jobPost.getCreatedBy().getEmail());
        workflowRequest.setFirstName(jobPost.getCreatedBy().getFirstName()); // Add first name
        workflowRequest.setLastName(jobPost.getCreatedBy().getLastName());   // Add last name
        workflowRequest.setPayload(Map.of(
                "applicantName", currentUser.getFirstName() + " " + currentUser.getLastName(),
                "applicantEmail", currentUser.getEmail(),
                "jobTitle", jobPost.getTitle(),
                "companyName", jobPost.getCompany(),
                "applicationId", savedApplication.getId().toString(),
                "applicationDate", savedApplication.getApplicationDate().toString()
        ));

        // Trigger notification workflow asynchronously (fire and forget)
        novuWorkflowService.triggerWorkflow(
            "application-received", // Workflow ID in Novu
            workflowRequest
        );

        return toSummaryDto(savedApplication);
    }

    @Override
    public void withdrawApplication(UUID applicationId) {
        User currentUser = organizationContext.getCurrentUser();
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getCandidate().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Application not found");
        }

        if (application.getStatus() != JobApplicationStatus.APPLIED && application.getStatus() != JobApplicationStatus.UNDER_REVIEW) {
            throw new BadRequestException("Application can only be withdrawn if its status is APPLIED or UNDER_REVIEW.");
        }
        application.setStatus(JobApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(application);
        jobApplicationAuditRepository.save(new JobApplicationAudit(application, JobApplicationStatus.WITHDRAWN, "Application withdrawn by candidate."));
    }

    private ApplicationSummaryDto toSummaryDto(JobApplication application) {
        ApplicationSummaryDto.JobSummaryDto jobSummary = new ApplicationSummaryDto.JobSummaryDto(
                application.getJobPost().getId(),
                application.getJobPost().getTitle(),
                application.getJobPost().getCompany()
        );
        return new ApplicationSummaryDto(
                application.getId(),
                application.getApplicationDate(),
                application.getStatus().name(),
                jobSummary
        );
    }

    private ApplicationDetailsDto toDetailsDto(JobApplication application) {
        List<JobApplicationAudit> auditHistory = jobApplicationAuditRepository.findByJobApplicationId(application.getId());
        List<ApplicationDetailsDto.CommunicationHistoryDto> communicationHistory = auditHistory.stream()
                .map(audit -> new ApplicationDetailsDto.CommunicationHistoryDto(
                        audit.getStatus().name(),
                        audit.getDate(),
                        audit.getMessage()
                ))
                .collect(Collectors.toList());

        User candidate = application.getCandidate();
        CandidateProfileDto candidateProfileDto = new CandidateProfileDto(
                candidate.getId(),
                candidate.getFirstName(),
                candidate.getLastName(),
                candidate.getEmail(),
                candidate.getProfileImageUrl(),
                candidate.getSummary(),
                candidate.getProfile()
        );

        return new ApplicationDetailsDto(
                application.getId(),
                application.getApplicationDate(),
                application.getStatus().name(),
                new JobPostDto(
                        application.getJobPost().getId(),
                        application.getJobPost().getTitle(),
                        application.getJobPost().getCompany(),
                        application.getJobPost().getJobType(),
                        application.getJobPost().getDatePosted(),
                        application.getJobPost().getDescription(),
                        application.getJobPost().getLocation(),
                        application.getJobPost().getRemote(),
                        application.getJobPost().getSalary(),
                        application.getJobPost().getExperienceLevel(),
                        application.getJobPost().getResponsibilities(),
                        application.getJobPost().getQualifications(),
                        application.getJobPost().getSkills(),
                        application.getJobPost().getStatus(),
                        application.getJobPost().getCreatedBy().getEmail(),
                        application.getJobPost().getCreatedAt(),
                        application.getJobPost().getUpdatedAt()
                ),
                candidateProfileDto,
                communicationHistory
        );
    }

    @Override
    public Page<EmployerApplicationSummaryDto> getApplicationsForJob(UUID jobId, UUID userId, Pageable pageable) {
        User user = organizationContext.getCurrentUser();
        JobPost jobPost = jobPostRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found"));

        // Use permission service for consistent authorization
        if (!permissionService.canView(user, jobPost)) {
            throw new UnauthorizedException("You are not authorized to view applications for this job post.");
        }

        // Additional check: must be hiring manager or recruiter
        if (user.getRole() != Role.HIRING_MANAGER && user.getRole() != Role.RECRUITER) {
            throw new UnauthorizedException("Only hiring managers and recruiters can view applications.");
        }

        Page<JobApplication> applications = jobApplicationRepository.findByJobPostId(jobId, pageable);

        return applications.map(application -> {
            User candidate = application.getCandidate();
            EmployerApplicationSummaryDto.CandidateDto candidateDto = new EmployerApplicationSummaryDto.CandidateDto(
                    candidate.getId(),
                    candidate.getFirstName() + " " + candidate.getLastName(),
                    candidate.getEmail(),
                    candidate.getProfileImageUrl()
            );
            return new EmployerApplicationSummaryDto(
                    application.getId(),
                    application.getApplicationDate(),
                    application.getStatus().name(),
                    candidateDto
            );
        });
    }

    @Override
    public ApplicationDetailsDto transitionApplicationStatus(UUID applicationId, JobApplicationStatus targetStatus, UUID userId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Job application not found"));

        // Authorization: Only Hiring Managers or Recruiters of the job post's organization can transition status
        JobPost jobPost = application.getJobPost();
        if (currentUser.getRole() != Role.HIRING_MANAGER && currentUser.getRole() != Role.RECRUITER) {
            throw new UnauthorizedException("Only Hiring Managers and Recruiters can transition application status.");
        }
        if (!permissionService.belongsToSameOrganization(currentUser, jobPost)) {
            throw new UnauthorizedException("You are not authorized to transition this application.");
        }

        // State Transition Validation
        if (!JobApplicationStateTransition.isValidTransition(application.getStatus(), targetStatus)) {
            throw new BadRequestException(String.format("Invalid application status transition from %s to %s",
                    application.getStatus(), targetStatus));
        }

        // Update status
        application.setStatus(targetStatus);
        JobApplication updatedApplication = jobApplicationRepository.save(application);

        // Audit the transition
        String auditMessage = String.format("Application status transitioned from %s to %s by user %s.",
                application.getStatus(), targetStatus, currentUser.getEmail());
        jobApplicationAuditRepository.save(new JobApplicationAudit(updatedApplication, targetStatus, auditMessage));

        // TODO: Trigger Novu notification for candidate about status change

        return toDetailsDto(updatedApplication);
    }
}
