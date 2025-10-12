package com.etalente.backend.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum StateTransition {
    // From DRAFT
    DRAFT_TO_OPEN(JobPostStatus.DRAFT, JobPostStatus.OPEN, "Publish job post"),
    DRAFT_TO_ARCHIVED(JobPostStatus.DRAFT, JobPostStatus.ARCHIVED, "Cancel draft without publishing"),

    // From OPEN
    OPEN_TO_CLOSED(JobPostStatus.OPEN, JobPostStatus.CLOSED, "Close/fill position"),
    OPEN_TO_DRAFT(JobPostStatus.OPEN, JobPostStatus.DRAFT, "Unpublish for major edits"),
    OPEN_TO_ARCHIVED(JobPostStatus.OPEN, JobPostStatus.ARCHIVED, "Cancel active posting"),

    // From CLOSED
    CLOSED_TO_ARCHIVED(JobPostStatus.CLOSED, JobPostStatus.ARCHIVED, "Archive completed posting"),
    CLOSED_TO_OPEN(JobPostStatus.CLOSED, JobPostStatus.OPEN, "Reopen position");

    // ARCHIVED is a terminal state - no transitions from it

    private final JobPostStatus fromStatus;
    private final JobPostStatus toStatus;
    private final String description;

    StateTransition(JobPostStatus fromStatus, JobPostStatus toStatus, String description) {
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.description = description;
    }

    public JobPostStatus getFromStatus() {
        return fromStatus;
    }

    public JobPostStatus getToStatus() {
        return toStatus;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if a transition from one status to another is valid
     */
    public static boolean isValidTransition(JobPostStatus from, JobPostStatus to) {
        return Arrays.stream(values())
                .anyMatch(t -> t.fromStatus == from && t.toStatus == to);
    }

    /**
     * Get all valid transitions from a given status
     */
    public static Set<StateTransition> getValidTransitionsFrom(JobPostStatus status) {
        return Arrays.stream(values())
                .filter(t -> t.fromStatus == status)
                .collect(Collectors.toSet());
    }

    /**
     * Get the transition between two statuses
     */
    public static StateTransition getTransition(JobPostStatus from, JobPostStatus to) {
        return Arrays.stream(values())
                .filter(t -> t.fromStatus == from && t.toStatus == to)
                .findFirst()
                .orElse(null);
    }
}