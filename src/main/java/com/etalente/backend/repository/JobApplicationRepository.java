package com.etalente.backend.repository;

import com.etalente.backend.model.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID>, JpaSpecificationExecutor<JobApplication> {

    Page<JobApplication> findByCandidateId(UUID candidateId, Pageable pageable);

    boolean existsByCandidateIdAndJobPostId(UUID candidateId, UUID jobPostId);

        int countByJobPostId(UUID jobPostId);

        int countByJobPostIdAndViewedByEmployerFalse(UUID jobPostId);

        Page<JobApplication> findByJobPostId(UUID jobPostId, Pageable pageable);

    }
