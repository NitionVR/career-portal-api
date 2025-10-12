package com.etalente.backend.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_post_state_audit")
@EntityListeners(AuditingEntityListener.class)
public class JobPostStateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_post_id", nullable = false)
    private JobPost jobPost;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false)
    private JobPostStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private JobPostStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @CreatedDate
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // Constructors
    public JobPostStateAudit() {
    }

    public JobPostStateAudit(JobPost jobPost, JobPostStatus fromStatus, JobPostStatus toStatus,
                             User changedBy, String reason) {
        this.jobPost = jobPost;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public JobPost getJobPost() {
        return jobPost;
    }

    public void setJobPost(JobPost jobPost) {
        this.jobPost = jobPost;
    }

    public JobPostStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(JobPostStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public JobPostStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(JobPostStatus toStatus) {
        this.toStatus = toStatus;
    }

    public User getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(User changedBy) {
        this.changedBy = changedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}