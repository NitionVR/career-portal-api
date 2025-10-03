package com.etalente.backend.dto;

import com.etalente.backend.model.InvitationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record RecruiterInvitationDto(
    UUID id,
    String email,
    InvitationStatus status,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {}
