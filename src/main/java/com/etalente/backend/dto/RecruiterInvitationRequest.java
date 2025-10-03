package com.etalente.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecruiterInvitationRequest(
    @NotBlank @Email String email,
    String personalMessage
) {}
