package com.etalente.backend.repository;

import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, UUID> {

    Page<JobPost> findByCreatedByEmail(String email, Pageable pageable);

    Page<JobPost> findByStatus(JobPostStatus status, Pageable pageable);

    @Query("SELECT jp FROM JobPost jp WHERE jp.status = 'OPEN' ORDER BY jp.createdAt DESC")
    Page<JobPost> findPublishedJobs(Pageable pageable);

    @Query("SELECT jp FROM JobPost jp WHERE " +
            "(LOWER(jp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(jp.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(jp.company) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "jp.status = 'OPEN'")
    Page<JobPost> searchJobPosts(String keyword, Pageable pageable);
}