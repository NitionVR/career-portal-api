package com.etalente.backend.repository;

import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, UUID>, JpaSpecificationExecutor<JobPost> {

    // Organization-filtered queries
    Page<JobPost> findByOrganization(Organization organization, Pageable pageable);

    Page<JobPost> findByOrganizationAndStatus(Organization organization, JobPostStatus status, Pageable pageable);

    Optional<JobPost> findByIdAndOrganization(UUID id, Organization organization);

    // User-specific queries (within organization)
    @Deprecated
    @Query("SELECT jp FROM JobPost jp WHERE jp.createdBy.email = :email AND jp.organization = :organization")
    Page<JobPost> findByCreatedByEmailAndOrganization(
            @Param("email") String email,
            @Param("organization") Organization organization,
            Pageable pageable);

    @Query("SELECT jp FROM JobPost jp WHERE jp.createdBy.id = :userId AND jp.organization = :organization")
    Page<JobPost> findByCreatedByIdAndOrganization(
            @Param("userId") UUID userId,
            @Param("organization") Organization organization,
            Pageable pageable);

    // Public queries (no organization filter needed)
    @Query("SELECT jp FROM JobPost jp WHERE jp.status = 'OPEN' ORDER BY jp.createdAt DESC")
    Page<JobPost> findPublishedJobs(Pageable pageable);

    @Query("SELECT jp FROM JobPost jp WHERE " +
            "(LOWER(jp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(jp.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(jp.company) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "jp.status = 'OPEN'")
    Page<JobPost> searchJobPosts(@Param("keyword") String keyword, Pageable pageable);

    // Keep legacy methods for backward compatibility (mark as deprecated)
    @Deprecated
    Page<JobPost> findByCreatedByEmail(String email, Pageable pageable);

    @Deprecated
    Page<JobPost> findByStatus(JobPostStatus status, Pageable pageable);
}