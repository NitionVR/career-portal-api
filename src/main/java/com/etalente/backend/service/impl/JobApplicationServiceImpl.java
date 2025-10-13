package com.etalente.backend.service.impl;

import com.etalente.backend.dto.ApplicationDetailsDto;
import com.etalente.backend.dto.ApplicationSummaryDto;
import com.etalente.backend.dto.JobPostDto;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.JobApplication;
import com.etalente.backend.model.JobApplicationStatus;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobApplicationRepository;
import com.etalente.backend.repository.JobApplicationSpecification;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.JobApplicationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import com.etalente.backend.model.JobApplicationAudit;
import com.etalente.backend.repository.JobApplicationAuditRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobPostRepository jobPostRepository;
    private final OrganizationContext organizationContext;
    private final JobApplicationAuditRepository jobApplicationAuditRepository;

    public JobApplicationServiceImpl(JobApplicationRepository jobApplicationRepository,
                                     JobPostRepository jobPostRepository,
                                     OrganizationContext organizationContext,
                                     JobApplicationAuditRepository jobApplicationAuditRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.jobPostRepository = jobPostRepository;
        this.organizationContext = organizationContext;
        this.jobApplicationAuditRepository = jobApplicationAuditRepository;
    }

    @Override
    public Page<ApplicationSummaryDto> getMyApplications(Pageable pageable, String search, String sort) {
        User currentUser = organizationContext.getCurrentUser();
        // TODO: Implement sort logic
        return jobApplicationRepository.findAll(JobApplicationSpecification.withFilters(currentUser.getId(), search, sort), pageable)
                .map(this::toSummaryDto);
    }

    @Override
    public ApplicationDetailsDto getApplicationDetails(UUID applicationId) {
        User currentUser = organizationContext.getCurrentUser();
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (!application.getCandidate().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Application not found");
        }

        return toDetailsDto(application);
    }

    @Override
    public ApplicationSummaryDto applyForJob(UUID jobPostId) {
        User currentUser = organizationContext.getCurrentUser();
        JobPost jobPost = jobPostRepository.findById(jobPostId)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found"));

        if (jobApplicationRepository.existsByCandidateIdAndJobPostId(currentUser.getId(), jobPostId)) {
            throw new BadRequestException("You have already applied for this job.");
        }

        JobApplication application = new JobApplication();
        application.setCandidate(currentUser);
        application.setJobPost(jobPost);
        application.setStatus(JobApplicationStatus.APPLIED);

        JobApplication savedApplication = jobApplicationRepository.save(application);

        jobApplicationAuditRepository.save(new JobApplicationAudit(savedApplication, JobApplicationStatus.APPLIED, "Application submitted."));

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

        application.setStatus(JobApplicationStatus.WITHDRAWN);
        jobApplicationRepository.save(application);
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
                communicationHistory
        );
    }
}
