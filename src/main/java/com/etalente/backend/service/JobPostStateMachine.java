package com.etalente.backend.service;

import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStateAudit;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.User;

import java.util.List;

public interface JobPostStateMachine {

    /**
     * Validate if a state transition is allowed
     */
    void validateTransition(JobPost jobPost, JobPostStatus targetStatus, User user);

    /**
     * Execute a state transition with audit logging
     */
    JobPost transitionState(JobPost jobPost, JobPostStatus targetStatus, User user, String reason);

    /**
     * Get the state change history for a job post
     */
    List<JobPostStateAudit> getStateHistory(JobPost jobPost);

    /**
     * Check if a job post can be published (has all required fields)
     */
    boolean canBePublished(JobPost jobPost);
}
