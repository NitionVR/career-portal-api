package com.etalente.backend.controller;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.StateAuditResponse;
import com.etalente.backend.dto.StateTransitionRequest;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.service.JobPostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/job-posts")
public class JobPostController {

    private final JobPostService jobPostService;

    public JobPostController(JobPostService jobPostService) {
        this.jobPostService = jobPostService;
    }

    @PostMapping
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<JobPostResponse> createJobPost(@Valid @RequestBody JobPostRequest request,
                                                         Authentication authentication) {
        JobPostResponse response = jobPostService.createJobPost(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostResponse> getJobPost(@PathVariable UUID id) {
        JobPostResponse response = jobPostService.getJobPost(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public Page<JobPostResponse> listJobPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String skillSearch,
            @RequestParam(required = false) List<String> experienceLevels,
            @RequestParam(required = false) List<String> jobTypes,
            @RequestParam(required = false) List<String> workTypes) {
        return jobPostService.listJobPosts(pageable, search, skillSearch, experienceLevels, jobTypes, workTypes);
    }

    @GetMapping("/my-posts")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public Page<JobPostResponse> listMyJobPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return jobPostService.listJobPostsByUser(authentication.getName(), pageable);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> updateJobPost(@PathVariable UUID id,
                                                         @Valid @RequestBody JobPostRequest request,
                                                         Authentication authentication) {
        JobPostResponse response = jobPostService.updateJobPost(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<Void> deleteJobPost(@PathVariable UUID id, Authentication authentication) {
        jobPostService.deleteJobPost(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // ============= STATE TRANSITION ENDPOINTS =============

    /**
     * Generic state transition endpoint with audit trail
     */
    @PatchMapping("/{id}/transition")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> transitionState(
            @PathVariable UUID id,
            @Valid @RequestBody StateTransitionRequest request,
            Authentication authentication) {
        JobPostResponse response = jobPostService.transitionJobPostState(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Convenience endpoint: Publish a job post
     */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> publishJobPost(@PathVariable UUID id,
                                                          Authentication authentication) {
        JobPostResponse response = jobPostService.publishJobPost(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Convenience endpoint: Close a job post
     */
    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> closeJobPost(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        JobPostResponse response = jobPostService.closeJobPost(id, reason, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Convenience endpoint: Reopen a closed job post
     */
    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> reopenJobPost(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        JobPostResponse response = jobPostService.reopenJobPost(id, reason, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Convenience endpoint: Archive a job post
     */
    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> archiveJobPost(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        JobPostResponse response = jobPostService.archiveJobPost(id, reason, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * @deprecated Use /transition endpoint instead
     */
    @Deprecated
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> updateJobPostStatus(@PathVariable UUID id,
                                                               @RequestParam String status,
                                                               Authentication authentication) {
        JobPostStatus newStatus = JobPostStatus.valueOf(status.toUpperCase());
        JobPostResponse response = jobPostService.updateJobPostStatus(id, newStatus, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Get state change history for a job post
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<List<StateAuditResponse>> getStateHistory(
            @PathVariable UUID id,
            Authentication authentication) {
        List<StateAuditResponse> history = jobPostService.getStateHistory(id, authentication.getName());
        return ResponseEntity.ok(history);
    }

    /**
     * Get available transitions for a job post based on current status
     */
    @GetMapping("/{id}/available-transitions")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<List<String>> getAvailableTransitions(@PathVariable UUID id) {
        List<String> transitions = jobPostService.getAvailableTransitions(id);
        return ResponseEntity.ok(transitions);
    }
}