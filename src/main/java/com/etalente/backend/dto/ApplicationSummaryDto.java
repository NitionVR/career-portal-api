package com.etalente.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApplicationSummaryDto(
        UUID id,
        LocalDateTime applicationDate,
        String status,
        JobSummaryDto job
) {
    public record JobSummaryDto(
            UUID id,
            String title,
            String company
    ) {}
}
