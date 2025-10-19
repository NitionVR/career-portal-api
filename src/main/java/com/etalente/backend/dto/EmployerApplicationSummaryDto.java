package com.etalente.backend.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EmployerApplicationSummaryDto(
        UUID id,
        LocalDateTime applicationDate,
        String status,
        CandidateDto candidate
) {
    public record CandidateDto(
            UUID id,
            String name,
            String email,
            String profileImageUrl
    ) {}
}
