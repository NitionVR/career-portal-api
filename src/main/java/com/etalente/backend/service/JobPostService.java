package com.etalente.backend.service;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.StateAuditResponse;
import com.etalente.backend.dto.StateTransitionRequest;
import com.etalente.backend.model.JobPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface JobPostService {
    JobPostResponse createJobPost(JobPostRequest request, String userEmail);
    JobPostResponse getJobPost(UUID id);
    Page<JobPostResponse> listJobPosts(Pageable pageable,
                                       String search,
                                       String skillSearch,
                                       List<String> experienceLevels,
                                       List<String> jobTypes,
                                       List<String> workTypes);
    Page<JobPostResponse> listJobPostsByUser(String userEmail, Pageable pageable);
    JobPostResponse updateJobPost(UUID id, JobPostRequest request, String userEmail);
    void deleteJobPost(UUID id, String userEmail);

    // State machine methods (updated)
    JobPostResponse transitionJobPostState(UUID id, StateTransitionRequest request, String userEmail);
    JobPostResponse publishJobPost(UUID id, String userEmail);
    JobPostResponse closeJobPost(UUID id, String reason, String userEmail);
    JobPostResponse reopenJobPost(UUID id, String reason, String userEmail);
    JobPostResponse archiveJobPost(UUID id, String reason, String userEmail);

    // Deprecated - use transitionJobPostState instead
    @Deprecated
    JobPostResponse updateJobPostStatus(UUID id, JobPostStatus newStatus, String userEmail);

    // Add these method signatures for state history
    List<StateAuditResponse> getStateHistory(UUID jobPostId, String userEmail);
    List<String> getAvailableTransitions(UUID jobPostId);
}