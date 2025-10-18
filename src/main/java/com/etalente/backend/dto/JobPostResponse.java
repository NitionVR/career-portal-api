package com.etalente.backend.dto;

import com.etalente.backend.model.JobPostStatus;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.UUID;

public record JobPostResponse(
        UUID id,
        String title,
        String company,
        String jobType,
        String datePosted,
        String description,
        JsonNode location,
        String remote,
        String salary,
        String experienceLevel,
        JsonNode responsibilities,
        JsonNode qualifications,
        JsonNode skills,
        JobPostStatus status,
        String createdByEmail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int applicantsCount,
        int newApplicantsCount,
        String companyLogoUrl
) {}