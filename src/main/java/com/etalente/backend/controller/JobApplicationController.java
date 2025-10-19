package com.etalente.backend.controller;

import com.etalente.backend.dto.ApplicationDetailsDto;
import com.etalente.backend.dto.ApplicationSummaryDto;
import com.etalente.backend.dto.EmployerApplicationSummaryDto;
import com.etalente.backend.service.JobApplicationService;
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
@RequestMapping("/api")
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    public JobApplicationController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @GetMapping("/applications/me")
    @PreAuthorize("hasRole('CANDIDATE')")
    public Page<ApplicationSummaryDto> getMyApplications(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort) {
        return jobApplicationService.getMyApplications(pageable, search, sort);
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'HIRING_MANAGER', 'RECRUITER')")
    public ApplicationDetailsDto getApplicationDetails(@PathVariable UUID id) {
        return jobApplicationService.getApplicationDetails(id);
    }

    @PostMapping("/job-posts/{id}/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApplicationSummaryDto> applyForJob(@PathVariable UUID id) {
        ApplicationSummaryDto application = jobApplicationService.applyForJob(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(application);
    }

    @DeleteMapping("/applications/{id}")
    @PreAuthorize("hasRole('CANDIDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdrawApplication(@PathVariable UUID id) {
        jobApplicationService.withdrawApplication(id);
    }

    @GetMapping("/job-posts/{jobId}/applications")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public Page<EmployerApplicationSummaryDto> getApplicationsForJob(
            @PathVariable UUID jobId,
            @PageableDefault(size = 20, sort = "applicationDate", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return jobApplicationService.getApplicationsForJob(jobId, userId, pageable);
    }
}
