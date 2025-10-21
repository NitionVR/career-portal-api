package com.etalente.backend.dto;

import com.etalente.backend.model.JobApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ApplicationTransitionRequest(
        @NotNull
        JobApplicationStatus targetStatus
) {
}
