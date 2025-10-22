package com.etalente.backend.controller;

import com.etalente.backend.dto.ApplicantSummaryDto;
import com.etalente.backend.dto.ErrorResponse;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.ApplicantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/applicants")
@Validated
@Tag(name = "Applicants", description = "Applicant management endpoints for employers")
public class ApplicantController {

    private static final String UUID_REGEX = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private static final int MAX_PAGE_SIZE = 100;

    private final ApplicantService applicantService;
    private final OrganizationContext organizationContext;

    public ApplicantController(ApplicantService applicantService,
                              OrganizationContext organizationContext) {
        this.applicantService = applicantService;
        this.organizationContext = organizationContext;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    @Operation(
        summary = "Get paginated list of applicants",
        description = "Retrieve a filtered and paginated list of job applicants for the current organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved applicants",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<Page<ApplicantSummaryDto>> getApplicants(
            @PageableDefault(size = 20, page = 0, sort = "applicationDate", direction = Sort.Direction.DESC)
            @Parameter(description = "Pagination parameters (page, size, sort)")
            Pageable pageable,

            @RequestParam(required = false)
            @Size(max = 100, message = "Search term must not exceed 100 characters")
            @Parameter(description = "General search term (candidate name or job title)")
            String search,

            @RequestParam(required = false)
            @Size(max = 100, message = "Skill search term must not exceed 100 characters")
            @Parameter(description = "Search term for candidate skills")
            String skillSearch,

            @RequestParam(required = false)
            @Pattern(regexp = UUID_REGEX, message = "Invalid UUID format for jobId")
            @Parameter(description = "Filter by specific job post ID")
            String jobId,

            @RequestParam(required = false)
            @Parameter(description = "Filter by application statuses")
            List<String> statuses,

            @RequestParam(required = false)
            @Min(value = 0, message = "Experience minimum must be at least 0")
            @Max(value = 50, message = "Experience minimum must not exceed 50")
            @Parameter(description = "Minimum years of experience")
            Integer experienceMin,

            @RequestParam(required = false)
            @Parameter(description = "Filter by education levels (OR logic)")
            List<String> education,

            @RequestParam(required = false)
            @Size(max = 100, message = "Location search term must not exceed 100 characters")
            @Parameter(description = "Filter by location")
            String location,

            @RequestParam(required = false)
            @Min(value = 0, message = "AI match score must be at least 0")
            @Max(value = 100, message = "AI match score must not exceed 100")
            @Parameter(description = "Minimum AI match score (future feature)")
            Integer aiMatchScoreMin) {

        log.info("GET /api/applicants called by user: {} with filters: search={}, jobId={}, statuses={}",
                organizationContext.getCurrentUser().getEmail(), search, jobId, statuses);

        // Validate page size
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException(
                String.format("Page size must not exceed %d", MAX_PAGE_SIZE)
            );
        }

        UUID organizationId = organizationContext.getCurrentUser().getOrganization().getId();

        Page<ApplicantSummaryDto> applicants = applicantService.getApplicants(
            pageable,
            search,
            skillSearch,
            jobId,
            statuses,
            experienceMin,
            education,
            location,
            aiMatchScoreMin,
            organizationId
        );

        log.info("Returning {} applicants (page {}/{})",
                applicants.getNumberOfElements(),
                applicants.getNumber() + 1,
                applicants.getTotalPages());

        return ResponseEntity.ok(applicants);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        log.warn("Validation error in ApplicantController: {}", ex.getMessage());

        List<String> errors = ex.getConstraintViolations().stream()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
            "Validation failed",
            errors,
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            request.getRequestURI(),
            null
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex,
            HttpServletRequest request) {

        log.warn("Bad request in ApplicantController: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
            ex.getMessage(),
            List.of(ex.getMessage()),
            HttpStatus.BAD_REQUEST.value(),
            LocalDateTime.now(),
            request.getRequestURI(),
            null
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }
}
