package com.etalente.backend.controller;

import com.etalente.backend.dto.ApplicantSummaryDto;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.ApplicantService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applicants")
public class ApplicantController {

    private final ApplicantService applicantService;
    private final OrganizationContext organizationContext;

    public ApplicantController(ApplicantService applicantService, OrganizationContext organizationContext) {
        this.applicantService = applicantService;
        this.organizationContext = organizationContext;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public Page<ApplicantSummaryDto> getApplicants(
            @PageableDefault(size = 20, sort = "applicationDate", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String skillSearch,
            @RequestParam(required = false) String jobId,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Integer experienceMin,
            @RequestParam(required = false) List<String> education,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer aiMatchScoreMin) {

        UUID organizationId = organizationContext.getCurrentUser().getOrganization().getId();

        return applicantService.getApplicants(pageable,
                search,
                skillSearch,
                jobId,
                statuses,
                experienceMin,
                education,
                location,
                aiMatchScoreMin,
                organizationId);
    }
}
