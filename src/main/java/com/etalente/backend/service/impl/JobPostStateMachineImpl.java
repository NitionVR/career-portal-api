package com.etalente.backend.service.impl;

import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.JobPostStateAuditRepository;
import com.etalente.backend.service.JobPostStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class JobPostStateMachineImpl implements JobPostStateMachine {

    private final JobPostRepository jobPostRepository;
    private final JobPostStateAuditRepository stateAuditRepository;

    public JobPostStateMachineImpl(JobPostRepository jobPostRepository,
                                   JobPostStateAuditRepository stateAuditRepository) {
        this.jobPostRepository = jobPostRepository;
        this.stateAuditRepository = stateAuditRepository;
    }

    @Override
    public void validateTransition(JobPost jobPost, JobPostStatus targetStatus, User user) {
        JobPostStatus currentStatus = jobPost.getStatus();

        // Check if trying to transition to the same status
        if (currentStatus == targetStatus) {
            throw new BadRequestException("Job post is already in " + targetStatus + " status");
        }

        // Check if transition is valid according to state machine rules
        if (!StateTransition.isValidTransition(currentStatus, targetStatus)) {
            throw new BadRequestException(
                    String.format("Invalid state transition from %s to %s", currentStatus, targetStatus)
            );
        }

        // Additional business rule validations
        validateBusinessRules(jobPost, targetStatus);
    }

    @Override
    public JobPost transitionState(JobPost jobPost, JobPostStatus targetStatus, User user, String reason) {
        // Validate the transition first
        validateTransition(jobPost, targetStatus, user);

        JobPostStatus fromStatus = jobPost.getStatus();

        // Update the job post status
        jobPost.setStatus(targetStatus);
        JobPost updatedJobPost = jobPostRepository.save(jobPost);

        // Create audit record
        JobPostStateAudit audit = new JobPostStateAudit(
                updatedJobPost,
                fromStatus,
                targetStatus,
                user,
                reason
        );
        stateAuditRepository.save(audit);

        return updatedJobPost;
    }

    @Override
    public List<JobPostStateAudit> getStateHistory(JobPost jobPost) {
        return stateAuditRepository.findByJobPostOrderByChangedAtDesc(jobPost);
    }

    @Override
    public boolean canBePublished(JobPost jobPost) {
        List<String> errors = new ArrayList<>();

        // Check required fields
        if (jobPost.getTitle() == null || jobPost.getTitle().trim().isEmpty()) {
            errors.add("Title is required");
        }
        if (jobPost.getDescription() == null || jobPost.getDescription().trim().isEmpty()) {
            errors.add("Description is required");
        }
        if (jobPost.getJobType() == null || jobPost.getJobType().trim().isEmpty()) {
            errors.add("Job type is required");
        }
        if (jobPost.getLocation() == null) {
            errors.add("Location is required");
        }
        if (jobPost.getExperienceLevel() == null || jobPost.getExperienceLevel().trim().isEmpty()) {
            errors.add("Experience level is required");
        }

        return errors.isEmpty();
    }

    /**
     * Validate business rules for specific state transitions
     */
    private void validateBusinessRules(JobPost jobPost, JobPostStatus targetStatus) {
        switch (targetStatus) {
            case OPEN:
                // Can only publish if all required fields are filled
                if (!canBePublished(jobPost)) {
                    throw new BadRequestException(
                            "Cannot publish job post: missing required fields (title, description, job type, location, experience level)"
                    );
                }
                break;

            case ARCHIVED:
                // ARCHIVED is a terminal state
                // No special validation needed, but could add warnings
                break;

            case CLOSED:
                // Could add validation like "must have at least one application"
                // For now, allow closing at any time
                break;

            case DRAFT:
                // Unpublishing back to draft - no special validation
                break;

            default:
                // No additional rules for other transitions
                break;
        }
    }
}
