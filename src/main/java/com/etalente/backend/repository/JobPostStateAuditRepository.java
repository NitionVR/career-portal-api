package com.etalente.backend.repository;

import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStateAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobPostStateAuditRepository extends JpaRepository<JobPostStateAudit, UUID> {

    /**
     * Get all state changes for a job post, ordered by most recent first
     */
    List<JobPostStateAudit> findByJobPostOrderByChangedAtDesc(JobPost jobPost);

    /**
     * Get state change history for a job post by ID
     */
    @Query("SELECT sa FROM JobPostStateAudit sa WHERE sa.jobPost.id = :jobPostId ORDER BY sa.changedAt DESC")
    List<JobPostStateAudit> findByJobPostId(@Param("jobPostId") UUID jobPostId);

    /**
     * Count state changes for a job post
     */
    long countByJobPost(JobPost jobPost);
}
