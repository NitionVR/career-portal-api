package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record HiringManagerRegistrationDto(
    @NotBlank String username,
    @NotBlank String companyName,
    @NotBlank String industry,
    @NotBlank String contactPerson,
    @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$") String contactNumber
) {}
