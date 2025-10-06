package com.etalente.backend.service;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.model.JobPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface JobPostService {
    JobPostResponse createJobPost(JobPostRequest request, String userEmail);
    JobPostResponse getJobPost(UUID id);
    Page<JobPostResponse> listJobPosts(Pageable pageable);
    Page<JobPostResponse> listJobPostsByUser(String userEmail, Pageable pageable);
    JobPostResponse updateJobPost(UUID id, JobPostRequest request, String userEmail);
    void deleteJobPost(UUID id, String userEmail);

    // New methods for status management
    JobPostResponse updateJobPostStatus(UUID id, JobPostStatus newStatus, String userEmail);
    JobPostResponse publishJobPost(UUID id, String userEmail);
    JobPostResponse closeJobPost(UUID id, String userEmail);
}