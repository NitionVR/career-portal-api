package com.etalente.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public record CandidateProfileDto(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String profileImageUrl,
        String summary,
        JsonNode profile
) {
}