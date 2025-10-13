package com.etalente.backend.repository;

import com.etalente.backend.model.JobApplicationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobApplicationAuditRepository extends JpaRepository<JobApplicationAudit, UUID> {

    List<JobApplicationAudit> findByJobApplicationId(UUID jobApplicationId);
}
