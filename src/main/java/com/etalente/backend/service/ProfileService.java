package com.etalente.backend.service;

import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.dto.UserDto;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.dto.ResumeDto;
import com.etalente.backend.dto.UploadUrlResponse;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
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

    UploadUrlResponse getResumeUploadUrl(UUID userId, String contentType, long contentLength, String fileName);
    ResumeDto addResumeToProfile(UUID userId, String resumeUrl, String fileName);
    void deleteResume(UUID userId, UUID resumeId);
    List<ResumeDto> getResumes(UUID userId);
}
