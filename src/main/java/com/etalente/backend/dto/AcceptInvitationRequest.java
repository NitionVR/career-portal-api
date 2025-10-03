package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AcceptInvitationRequest(
    @NotBlank String username,
    @NotBlank String firstName,
    @NotBlank String lastName,
    @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String contactNumber,
    String password // Optional - for if they want to set one
) {}

