package com.etalente.backend.dto;

import com.etalente.backend.model.JobPostStatus;
import jakarta.validation.constraints.NotNull;

public record StateTransitionRequest(
        @NotNull(message = "Target status is required")
        JobPostStatus targetStatus,

        String reason
) {
}
