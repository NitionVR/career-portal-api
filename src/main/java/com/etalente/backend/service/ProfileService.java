package com.etalente.backend.service;

import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.dto.UserDto;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

public interface ProfileService {
    void updateProfile(UUID userId, Map<String, String> profileData);
    VerifyTokenResponse completeProfile(UUID userId, CompleteProfileRequest request);
    UserDto updateProfileImage(String userId, String profileImageUrl);
    void deleteProfileImage(String userId);
    UserDto getUserProfile(String userId);
    JsonNode getFullProfile(UUID userId);
    JsonNode updateFullProfile(UUID userId, JsonNode profile);
}
