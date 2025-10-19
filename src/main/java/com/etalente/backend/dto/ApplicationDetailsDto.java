package com.etalente.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ApplicationDetailsDto(
        UUID id,
        LocalDateTime applicationDate,
        String status,
        JobPostDto job,
        CandidateProfileDto candidateProfile,
        List<CommunicationHistoryDto> communicationHistory
) {
    public record CommunicationHistoryDto(
            String status,
            LocalDateTime date,
            String message
    ) {}
}
