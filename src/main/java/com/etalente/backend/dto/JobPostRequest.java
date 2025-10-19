package com.etalente.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record JobPostRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,

        @NotBlank(message = "Company is required")
        @Size(max = 255, message = "Company name cannot exceed 255 characters")
        String company,

        @Pattern(regexp = "Full-time|Part-time|Contract|Internship",
                message = "Job type must be Full-time, Part-time, Contract, or Internship")
        String jobType,

        @NotBlank(message = "Description is required")
        @Size(min = 50, max = 5000, message = "Description must be between 50 and 5000 characters")
        String description,

        LocationDto location,

        @Pattern(regexp = "Remote|Hybrid|On-site",
                message = "Remote must be Remote, Hybrid, or On-site")
        String remote,

        String salary,

        @Pattern(regexp = "(?i)Entry-level|Mid-level|Senior-level|Executive",
                message = "Invalid experience level")
        String experienceLevel,

        @NotNull(message = "Responsibilities are required")
        @Size(min = 1, message = "At least one responsibility is required")
        List<String> responsibilities,

        @NotNull(message = "Qualifications are required")
        @Size(min = 1, message = "At least one qualification is required")
        List<String> qualifications,

        List<SkillDto> skills
) {}