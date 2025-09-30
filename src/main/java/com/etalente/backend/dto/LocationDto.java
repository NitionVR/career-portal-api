package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record LocationDto(
        String address,
        String postalCode,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Country code is required")
        String countryCode,

        String region
) {}