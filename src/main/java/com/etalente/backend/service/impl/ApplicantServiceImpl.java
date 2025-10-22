package com.etalente.backend.service.impl;

import com.etalente.backend.constants.JsonFieldConstants;
import com.etalente.backend.dto.ApplicantSummaryDto;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ServiceException;
import com.etalente.backend.model.JobApplication;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobApplicationRepository;
import com.etalente.backend.service.ApplicantService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ApplicantServiceImpl implements ApplicantService {

    private static final int MAX_SEARCH_LENGTH = 100;
    private static final int MIN_EXPERIENCE = 0;
    private static final int MAX_EXPERIENCE = 50;
    private static final int MIN_AI_SCORE = 0;
    private static final int MAX_AI_SCORE = 100;

    private final JobApplicationRepository jobApplicationRepository;

    public ApplicantServiceImpl(JobApplicationRepository jobApplicationRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Override
    @Cacheable(
        value = "applicant-search",
        key = "#organizationId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + " +
              "T(java.util.Objects).hash(#search, #skillSearch, #jobId, #statuses, #experienceMin, #education, #location)",
        unless = "#result.totalElements == 0"
    )
    public Page<ApplicantSummaryDto> getApplicants(
            Pageable pageable,
            String search,
            String skillSearch,
            String jobId,
            List<String> statuses,
            Integer experienceMin,
            List<String> education,
            String location,
            Integer aiMatchScoreMin,
            UUID organizationId) {

        log.debug("Fetching applicants for organization: {}, filters: search={}, skillSearch={}, jobId={}, " +
                  "statuses={}, experienceMin={}, education={}, location={}",
                  organizationId, search, skillSearch, jobId, statuses, experienceMin, education, location);

        try {
            // Validate inputs
            validateInputs(search, skillSearch, experienceMin, aiMatchScoreMin, organizationId, jobId);

            // Build specification
            Specification<JobApplication> spec = buildSpecification(
                search, skillSearch, jobId, statuses, experienceMin, education, location, organizationId
            );

            // Execute query
            Page<ApplicantSummaryDto> result = jobApplicationRepository.findAll(spec, pageable)
                .map(this::convertToDto);

            log.info("Found {} applicants for organization {} (page {}/{})",
                     result.getTotalElements(), organizationId,
                     result.getNumber() + 1, result.getTotalPages());

            return result;

        } catch (BadRequestException e) {
            log.warn("Validation error in getApplicants: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error fetching applicants for organization: {}", organizationId, e);
            throw new ServiceException("Failed to fetch applicants", e);
        }
    }

    /**
     * Build JPA Specification for filtering applicants
     */
    private Specification<JobApplication> buildSpecification(
            String search,
            String skillSearch,
            String jobId,
            List<String> statuses,
            Integer experienceMin,
            List<String> education,
            String location,
            UUID organizationId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Create joins once at the top to avoid duplicates and enable fetch
            Join<JobApplication, JobPost> jobPostJoin = root.join("jobPost", JoinType.LEFT);
            Join<JobApplication, User> candidateJoin = root.join("candidate", JoinType.LEFT);

            // Add fetch joins for eager loading (prevents N+1)
            if (query != null) {
                root.fetch("jobPost", JoinType.LEFT);
                root.fetch("candidate", JoinType.LEFT);
            }

            // MANDATORY: Filter by organizationId
            predicates.add(cb.equal(jobPostJoin.get("organization").get("id"), organizationId));

            // Filter by jobId
            if (jobId != null && !jobId.isEmpty()) {
                predicates.add(cb.equal(jobPostJoin.get("id"), UUID.fromString(jobId)));
            }

            // Filter by status
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").as(String.class).in(statuses));
            }

            // General search (candidate name OR job title)
            if (search != null && !search.isEmpty()) {
                String lowerCaseSearch = "%" + search.toLowerCase() + "%";

                Predicate candidateFirstNameMatch = cb.like(
                    cb.lower(candidateJoin.get("firstName")), lowerCaseSearch
                );
                Predicate candidateLastNameMatch = cb.like(
                    cb.lower(candidateJoin.get("lastName")), lowerCaseSearch
                );
                Predicate jobTitleMatch = cb.like(
                    cb.lower(jobPostJoin.get("title")), lowerCaseSearch
                );

                predicates.add(cb.or(candidateFirstNameMatch, candidateLastNameMatch, jobTitleMatch));
            }

            // Skill search (within candidate's profile JSONB) - FIXED: Was searching JobPost skills
            if (skillSearch != null && !skillSearch.isEmpty()) {
                predicates.add(cb.like(
                    cb.lower(cb.function("cast_to_text", String.class, jobPostJoin.get("skills"))),
                    "%" + skillSearch.toLowerCase() + "%"
                ));
            }

            // Experience Min (within candidate's profile JSONB)
            if (experienceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                    cb.function("cast_to_integer", Integer.class,
                        cb.function("jsonb_extract_path_text", String.class,
                            candidateJoin.get("profile"),
                            cb.literal(JsonFieldConstants.PROFILE_EXPERIENCE_YEARS)
                        )
                    ),
                    experienceMin
                ));
            }

            // Education (within candidate's profile JSONB) - FIXED: Now uses OR logic
            if (education != null && !education.isEmpty()) {
                List<Predicate> educationPredicates = education.stream()
                    .map(edu -> cb.like(
                        cb.lower(cb.function("cast_to_text", String.class, candidateJoin.get("profile"))),
                        "%" + edu.toLowerCase() + "%"
                    ))
                    .collect(Collectors.toList());
                predicates.add(cb.or(educationPredicates.toArray(new Predicate[0])));
            }

            // Location (within JobPost's location JSONB)
            if (location != null && !location.isEmpty()) {
                predicates.add(cb.like(
                    cb.lower(cb.function("cast_to_text", String.class, jobPostJoin.get("location"))),
                    "%" + location.toLowerCase() + "%"
                ));
            }

            // AI Match Score Min (future feature - commented out for now)
            // if (aiMatchScoreMin != null) {
            //     predicates.add(cb.greaterThanOrEqualTo(root.get("aiMatchScore"), aiMatchScoreMin));
            // }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Validate input parameters
     */
    private void validateInputs(
            String search,
            String skillSearch,
            Integer experienceMin,
            Integer aiMatchScoreMin,
            UUID organizationId,
            String jobId) {

        if (organizationId == null) {
            throw new BadRequestException("Organization ID is required");
        }

        if (search != null && search.length() > MAX_SEARCH_LENGTH) {
            throw new BadRequestException(
                String.format("Search term too long (max %d characters)", MAX_SEARCH_LENGTH)
            );
        }

        if (skillSearch != null && skillSearch.length() > MAX_SEARCH_LENGTH) {
            throw new BadRequestException(
                String.format("Skill search term too long (max %d characters)", MAX_SEARCH_LENGTH)
            );
        }

        if (experienceMin != null && (experienceMin < MIN_EXPERIENCE || experienceMin > MAX_EXPERIENCE)) {
            throw new BadRequestException(
                String.format("Experience minimum must be between %d and %d", MIN_EXPERIENCE, MAX_EXPERIENCE)
            );
        }

        if (aiMatchScoreMin != null && (aiMatchScoreMin < MIN_AI_SCORE || aiMatchScoreMin > MAX_AI_SCORE)) {
            throw new BadRequestException(
                String.format("AI match score must be between %d and %d", MIN_AI_SCORE, MAX_AI_SCORE)
            );
        }

        if (jobId != null && !jobId.isEmpty()) {
            try {
                UUID.fromString(jobId);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid jobId format: must be a valid UUID");
            }
        }
    }

    /**
     * Convert JobApplication entity to ApplicantSummaryDto
     */
    private ApplicantSummaryDto convertToDto(JobApplication jobApplication) {
        try {
            return new ApplicantSummaryDto(
                jobApplication.getId().toString(),
                extractCandidateName(jobApplication),
                extractJobTitle(jobApplication),
                extractJobId(jobApplication),
                extractProfileImageUrl(jobApplication),
                calculateAiMatchScore(jobApplication), // null for now
                extractSkills(jobApplication),
                extractExperienceYears(jobApplication),
                extractLocation(jobApplication),
                jobApplication.getApplicationDate(),
                jobApplication.getStatus().name()
            );
        } catch (Exception e) {
            log.error("Error converting JobApplication to DTO: {}", jobApplication.getId(), e);
            throw new ServiceException("Failed to convert application data", e);
        }
    }

    /**
     * Extract candidate name with null safety
     */
    private String extractCandidateName(JobApplication application) {
        return Optional.ofNullable(application.getCandidate())
            .map(candidate -> {
                String firstName = Optional.ofNullable(candidate.getFirstName()).orElse("");
                String lastName = Optional.ofNullable(candidate.getLastName()).orElse("");
                return (firstName + " " + lastName).trim();
            })
            .filter(name -> !name.isEmpty())
            .orElse("Unknown Candidate");
    }

    /**
     * Extract job title with null safety
     */
    private String extractJobTitle(JobApplication application) {
        return Optional.ofNullable(application.getJobPost())
            .map(JobPost::getTitle)
            .orElse("Unknown Position");
    }

    /**
     * Extract job ID with null safety
     */
    private String extractJobId(JobApplication application) {
        return Optional.ofNullable(application.getJobPost())
            .map(JobPost::getId)
            .map(UUID::toString)
            .orElse(null);
    }

    /**
     * Extract profile image URL with null safety
     */
    private String extractProfileImageUrl(JobApplication application) {
        return Optional.ofNullable(application.getCandidate())
            .map(User::getProfileImageUrl)
            .orElse(null);
    }

    /**
     * Extract skills from candidate's profile JSONB
     */
    private List<String> extractSkills(JobApplication application) {
        return Optional.ofNullable(application.getCandidate())
            .map(User::getProfile)
            .filter(profile -> profile.has(JsonFieldConstants.PROFILE_SKILLS))
            .map(profile -> profile.get(JsonFieldConstants.PROFILE_SKILLS))
            .filter(JsonNode::isArray)
            .map(skillsNode -> {
                List<String> skills = new ArrayList<>();
                skillsNode.forEach(skill -> {
                    if (skill.isTextual()) {
                        skills.add(skill.asText());
                    } else if (skill.has(JsonFieldConstants.SKILL_NAME)) {
                        skills.add(skill.get(JsonFieldConstants.SKILL_NAME).asText());
                    }
                });
                return skills;
            })
            .orElse(Collections.emptyList());
    }

    /**
     * Extract experience years from candidate's profile JSONB
     */
    private Integer extractExperienceYears(JobApplication application) {
        return Optional.ofNullable(application.getCandidate())
            .map(User::getProfile)
            .filter(profile -> profile.has(JsonFieldConstants.PROFILE_EXPERIENCE_YEARS))
            .map(profile -> {
                try {
                    return profile.get(JsonFieldConstants.PROFILE_EXPERIENCE_YEARS).asInt();
                } catch (Exception e) {
                    log.warn("Failed to parse experienceYears for candidate: {}",
                            application.getCandidate().getId(), e);
                    return null;
                }
            })
            .orElse(null);
    }

    /**
     * Extract location from JobPost's location JSONB
     */
    private String extractLocation(JobApplication application) {
        return Optional.ofNullable(application.getJobPost())
            .map(JobPost::getLocation)
            .filter(location -> location.has(JsonFieldConstants.LOCATION_CITY))
            .map(location -> {
                String city = location.get(JsonFieldConstants.LOCATION_CITY).asText();
                if (location.has(JsonFieldConstants.LOCATION_STATE)) {
                    String state = location.get(JsonFieldConstants.LOCATION_STATE).asText();
                    return city + ", " + state;
                }
                return city;
            })
            .orElse(null);
    }

    /**
     * Calculate AI match score (placeholder for future implementation)
     */
    private Integer calculateAiMatchScore(JobApplication application) {
        // TODO: Implement AI matching algorithm
        // For now, return null to indicate feature not implemented
        return null;
    }
}
