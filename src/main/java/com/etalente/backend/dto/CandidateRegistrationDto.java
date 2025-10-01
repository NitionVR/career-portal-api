package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CandidateRegistrationDto(
    @NotBlank String username,
    @NotBlank String firstName,
    @NotBlank String lastName,
    String gender,
    String race,
    String disability,
    @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String contactNumber,
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String alternateContactNumber
) {}
