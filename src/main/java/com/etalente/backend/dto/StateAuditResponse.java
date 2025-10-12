package com.etalente.backend.dto;

import com.etalente.backend.model.JobPostStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StateAuditResponse(
        UUID id,
        JobPostStatus fromStatus,
        JobPostStatus toStatus,
        String changedByEmail,
        String reason,
        LocalDateTime changedAt
) {
}
