package com.etalente.backend.controller;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
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
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return jobPostService.listJobPosts(pageable);
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

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> updateJobPostStatus(@PathVariable UUID id,
                                                               @RequestParam String status,
                                                               Authentication authentication) {
        JobPostStatus newStatus = JobPostStatus.valueOf(status.toUpperCase());
        JobPostResponse response = jobPostService.updateJobPostStatus(id, newStatus, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> publishJobPost(@PathVariable UUID id,
                                                          Authentication authentication) {
        JobPostResponse response = jobPostService.publishJobPost(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> closeJobPost(@PathVariable UUID id,
                                                        Authentication authentication) {
        JobPostResponse response = jobPostService.closeJobPost(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}