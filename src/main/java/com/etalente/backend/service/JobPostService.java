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
    JobPostResponse createJobPost(JobPostRequest request, UUID userId);
    JobPostResponse getJobPost(UUID id);
    Page<JobPostResponse> listJobPosts(Pageable pageable,
                                       String search,
                                       String skillSearch,
                                       List<String> experienceLevels,
                                       List<String> jobTypes,
                                       List<String> workTypes);
    Page<JobPostResponse> listJobPostsByUser(UUID userId, Pageable pageable);
    JobPostResponse updateJobPost(UUID id, JobPostRequest request, UUID userId);
    void deleteJobPost(UUID id, UUID userId);

    // State machine methods (updated)
    JobPostResponse transitionJobPostState(UUID id, StateTransitionRequest request, UUID userId);
    JobPostResponse publishJobPost(UUID id, UUID userId);
    JobPostResponse closeJobPost(UUID id, String reason, UUID userId);
    JobPostResponse reopenJobPost(UUID id, String reason, UUID userId);
    JobPostResponse archiveJobPost(UUID id, String reason, UUID userId);

    // Deprecated - use transitionJobPostState instead
    @Deprecated
    JobPostResponse updateJobPostStatus(UUID id, JobPostStatus newStatus, UUID userId);

    // Add these method signatures for state history
    List<StateAuditResponse> getStateHistory(UUID jobPostId, UUID userId);
    List<String> getAvailableTransitions(UUID jobPostId);
}