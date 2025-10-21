package com.etalente.backend.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum JobApplicationStateTransition {
    // From APPLIED
    APPLIED_TO_UNDER_REVIEW(JobApplicationStatus.APPLIED, JobApplicationStatus.UNDER_REVIEW, "Application moved to under review"),
    APPLIED_TO_REJECTED(JobApplicationStatus.APPLIED, JobApplicationStatus.REJECTED, "Application rejected"),
    APPLIED_TO_WITHDRAWN(JobApplicationStatus.APPLIED, JobApplicationStatus.WITHDRAWN, "Application withdrawn by candidate"),

    // From UNDER_REVIEW
    UNDER_REVIEW_TO_INTERVIEW_SCHEDULED(JobApplicationStatus.UNDER_REVIEW, JobApplicationStatus.INTERVIEW_SCHEDULED, "Interview scheduled"),
    UNDER_REVIEW_TO_REJECTED(JobApplicationStatus.UNDER_REVIEW, JobApplicationStatus.REJECTED, "Application rejected after review"),
    UNDER_REVIEW_TO_WITHDRAWN(JobApplicationStatus.UNDER_REVIEW, JobApplicationStatus.WITHDRAWN, "Application withdrawn by candidate"),

    // From INTERVIEW_SCHEDULED
    INTERVIEW_SCHEDULED_TO_OFFER_EXTENDED(JobApplicationStatus.INTERVIEW_SCHEDULED, JobApplicationStatus.OFFER_EXTENDED, "Offer extended"),
    INTERVIEW_SCHEDULED_TO_REJECTED(JobApplicationStatus.INTERVIEW_SCHEDULED, JobApplicationStatus.REJECTED, "Application rejected after interview"),
    INTERVIEW_SCHEDULED_TO_WITHDRAWN(JobApplicationStatus.INTERVIEW_SCHEDULED, JobApplicationStatus.WITHDRAWN, "Application withdrawn by candidate"),

    // From OFFER_EXTENDED
    OFFER_EXTENDED_TO_HIRED(JobApplicationStatus.OFFER_EXTENDED, JobApplicationStatus.HIRED, "Candidate hired"),
    OFFER_EXTENDED_TO_REJECTED(JobApplicationStatus.OFFER_EXTENDED, JobApplicationStatus.REJECTED, "Offer declined or withdrawn"),
    OFFER_EXTENDED_TO_WITHDRAWN(JobApplicationStatus.OFFER_EXTENDED, JobApplicationStatus.WITHDRAWN, "Application withdrawn by candidate");

    // HIRED, REJECTED, WITHDRAWN are terminal states - no transitions from them

    private final JobApplicationStatus fromStatus;
    private final JobApplicationStatus toStatus;
    private final String description;

    JobApplicationStateTransition(JobApplicationStatus fromStatus, JobApplicationStatus toStatus, String description) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.description = description;
    }

    public JobApplicationStatus getFromStatus() {
        return fromStatus;
    }

    public JobApplicationStatus getToStatus() {
        return toStatus;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if a transition from one status to another is valid
     */
    public static boolean isValidTransition(JobApplicationStatus from, JobApplicationStatus to) {
        return Arrays.stream(values())
                .anyMatch(t -> t.fromStatus == from && t.toStatus == to);
    }

    /**
     * Get all valid transitions from a given status
     */
    public static Set<JobApplicationStateTransition> getValidTransitionsFrom(JobApplicationStatus status) {
        return Arrays.stream(values())
                .filter(t -> t.fromStatus == status)
                .collect(Collectors.toSet());
    }

    /**
     * Get the transition between two statuses
     */
    public static JobApplicationStateTransition getTransition(JobApplicationStatus from, JobApplicationStatus to) {
        return Arrays.stream(values())
                .filter(t -> t.fromStatus == from && t.toStatus == to)
                .findFirst()
                .orElse(null);
    }
}
