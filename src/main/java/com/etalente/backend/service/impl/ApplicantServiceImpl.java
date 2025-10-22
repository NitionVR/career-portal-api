package com.etalente.backend.service.impl;

import com.etalente.backend.dto.ApplicantSummaryDto;
import com.etalente.backend.model.JobApplication;
import com.etalente.backend.repository.JobApplicationRepository;
import com.etalente.backend.service.ApplicantService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ApplicantServiceImpl implements ApplicantService {

    private final JobApplicationRepository jobApplicationRepository;
    private final ObjectMapper objectMapper;

    public ApplicantServiceImpl(JobApplicationRepository jobApplicationRepository, ObjectMapper objectMapper) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Page<ApplicantSummaryDto> getApplicants(Pageable pageable,
                                                   String search,
                                                   String skillSearch,
                                                   String jobId,
                                                   List<String> statuses,
                                                   Integer experienceMin,
                                                   List<String> education,
                                                   String location,
                                                   Integer aiMatchScoreMin,
                                                   UUID organizationId) {

        Specification<JobApplication> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by organizationId (mandatory for employer views)
            Join<Object, Object> jobPostJoin = root.join("jobPost");
            predicates.add(cb.equal(jobPostJoin.get("organization").get("id"), organizationId));

            // Filter by jobId
            if (jobId != null && !jobId.isEmpty()) {
                predicates.add(cb.equal(jobPostJoin.get("id"), UUID.fromString(jobId)));
            }

            // Filter by status
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").as(String.class).in(statuses));
            }

            // General search (candidate name, job title)
            if (search != null && !search.isEmpty()) {
                String lowerCaseSearch = search.toLowerCase();
                Join<Object, Object> candidateJoin = root.join("candidate");
                Predicate candidateNameMatch = cb.or(
                        cb.like(cb.lower(candidateJoin.get("firstName")), "%" + lowerCaseSearch + "%"),
                        cb.like(cb.lower(candidateJoin.get("lastName")), "%" + lowerCaseSearch + "%")
                );
                Predicate jobTitleMatch = cb.like(cb.lower(jobPostJoin.get("title")), "%" + lowerCaseSearch + "%");
                predicates.add(cb.or(candidateNameMatch, jobTitleMatch));
            }

            // Skill search (within JobPost's skills JSONB)
            if (skillSearch != null && !skillSearch.isEmpty()) {
                // This checks if the string representation of the entire skills JSONB contains the skillSearch term
                // This is a simplified approach and might not be precise for complex JSON structures
                predicates.add(cb.like(cb.lower(cb.function("cast_to_text", String.class, jobPostJoin.get("skills"))), "%" + skillSearch.toLowerCase() + "%"));
            }

            // Experience Min (within User's profile JSONB)
            if (experienceMin != null) {
                Join<Object, Object> candidateJoin = root.join("candidate");
                predicates.add(cb.greaterThanOrEqualTo(
                    cb.function("cast_to_integer", Integer.class,
                        cb.function("jsonb_extract_path_text", String.class, candidateJoin.get("profile"), cb.literal("experienceYears"))
                    ),
                    experienceMin
                ));
            }

            // Education (within User's profile JSONB)
            if (education != null && !education.isEmpty()) {
                Join<Object, Object> candidateJoin = root.join("candidate");
                for (String edu : education) {
                    // This checks if the string representation of the entire profile JSONB contains the education term
                    // This is a simplified approach and might not be precise for complex JSON structures
                    predicates.add(cb.like(cb.lower(cb.function("cast_to_text", String.class, candidateJoin.get("profile"))), "%" + edu.toLowerCase() + "%"));
                }
            }

            // Location (within JobPost's location JSONB)
            if (location != null && !location.isEmpty()) {
                // This checks if the string representation of the entire location JSONB contains the location term
                // This is a simplified approach and might not be precise for complex JSON structures
                predicates.add(cb.like(cb.lower(cb.function("cast_to_text", String.class, jobPostJoin.get("location"))), "%" + location.toLowerCase() + "%"));
            }

            // AI Match Score Min (assuming it's a direct field on JobApplication or can be joined)
            // if (aiMatchScoreMin != null) {
            //     // Assuming aiMatchScore is a direct field on JobApplication for now
            //     // If it's a calculated field, this will need to be adjusted
            //     predicates.add(cb.greaterThanOrEqualTo(root.get("aiMatchScore"), aiMatchScoreMin));
            // }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return jobApplicationRepository.findAll(spec, pageable)
                .map(this::convertToDto);
    }

    private ApplicantSummaryDto convertToDto(JobApplication jobApplication) {
        // Extract skills from JobPost JSONB
        List<String> skills = new ArrayList<>();
        JsonNode skillsNode = jobApplication.getJobPost().getSkills();
        if (skillsNode != null && skillsNode.isArray()) {
            for (JsonNode skill : skillsNode) {
                if (skill.has("name")) {
                    skills.add(skill.get("name").asText());
                }
            }
        }

        // Extract experienceYears from Candidate's profile JSONB
        Integer experienceYears = null;
        JsonNode candidateProfileNode = jobApplication.getCandidate().getProfile();
        if (candidateProfileNode != null && candidateProfileNode.has("experienceYears")) {
            experienceYears = candidateProfileNode.get("experienceYears").asInt();
        }

        // Extract location from JobPost JSONB
        String location = null;
        JsonNode jobLocationNode = jobApplication.getJobPost().getLocation();
        if (jobLocationNode != null && jobLocationNode.has("city")) {
            location = jobLocationNode.get("city").asText(); // Simplified to city for now
        }

        // AI Match Score - assuming it's a direct field on JobApplication for now
        // If not, it needs to be calculated or fetched from another source
        Integer aiMatchScore = null; // Placeholder

        return new ApplicantSummaryDto(
                jobApplication.getId().toString(),
                jobApplication.getCandidate().getFirstName() + " " + jobApplication.getCandidate().getLastName(),
                jobApplication.getJobPost().getTitle(),
                jobApplication.getJobPost().getId().toString(),
                jobApplication.getCandidate().getProfileImageUrl(),
                aiMatchScore, // Placeholder
                skills,
                experienceYears,
                location,
                jobApplication.getApplicationDate(),
                jobApplication.getStatus().name()
        );
    }
}
