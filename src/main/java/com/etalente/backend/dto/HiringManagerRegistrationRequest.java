package com.etalente.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record HiringManagerRegistrationRequest(
    @Valid @NotNull HiringManagerRegistrationDto hiringManager
) {}
