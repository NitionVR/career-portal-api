package com.etalente.backend.controller;

import com.etalente.backend.dto.*;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.OrganizationService;
import com.etalente.backend.service.S3Service;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final S3Service s3Service;
    private final OrganizationContext organizationContext;

    public OrganizationController(OrganizationService organizationService,
                                 S3Service s3Service,
                                 OrganizationContext organizationContext) {
        this.organizationService = organizationService;
        this.s3Service = s3Service;
        this.organizationContext = organizationContext;
    }

    /**
     * P2: Get pre-signed URL for company logo upload
     * POST /api/organization/logo/upload-url
     */
    @PostMapping("/logo/upload-url")
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<UploadUrlResponse> getLogoUploadUrl(
            @Valid @RequestBody UploadUrlRequest request) {

        UploadUrlResponse response = s3Service.generatePresignedUploadUrl(
            "company-logos",
            request.getContentType(),
            request.getContentLength()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * P2: Update organization with logo URL
     * PUT /api/organization/logo
     */
    @PutMapping("/logo")
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<OrganizationDto> updateLogo(
            @Valid @RequestBody UpdateCompanyLogoRequest request) {

        String userId = organizationContext.getCurrentUser().getId().toString();
        OrganizationDto updatedOrg = organizationService.updateCompanyLogo(
            userId,
            request.getCompanyLogoUrl()
        );

        return ResponseEntity.ok(updatedOrg);
    }

    /**
     * P2: Delete company logo
     * DELETE /api/organization/logo
     */
    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<Void> deleteLogo() {
        String userId = organizationContext.getCurrentUser().getId().toString();
        organizationService.deleteCompanyLogo(userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Get current organization
     * GET /api/organization/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('HIRING_MANAGER') or hasRole('RECRUITER')")
    public ResponseEntity<OrganizationDto> getCurrentOrganization() {
        String userId = organizationContext.getCurrentUser().getId().toString();
        OrganizationDto org = organizationService.getOrganizationByUserId(userId);

        return ResponseEntity.ok(org);
    }
}
