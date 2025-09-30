package com.etalente.backend.repository;

import com.etalente.backend.model.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, UUID> {
}
