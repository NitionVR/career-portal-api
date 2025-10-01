package com.etalente.backend.dto;

import com.etalente.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(
    @NotBlank @Email String email,
    @NotNull Role role
) {}
