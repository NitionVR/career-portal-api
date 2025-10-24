package com.etalente.backend.controller;

import com.etalente.backend.dto.*;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.ProfileService;
import com.etalente.backend.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final S3Service s3Service;
    private final OrganizationContext organizationContext;

    public ProfileController(ProfileService profileService, S3Service s3Service, OrganizationContext organizationContext) {
        this.profileService = profileService;
        this.s3Service = s3Service;
        this.organizationContext = organizationContext;
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updateProfile(@RequestBody Map<String, String> profileData) {
        UUID userId = organizationContext.getCurrentUser().getId();
        profileService.updateProfile(userId, profileData);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/complete")
    public ResponseEntity<VerifyTokenResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        UUID userId = organizationContext.getCurrentUser().getId();
        VerifyTokenResponse response = profileService.completeProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/avatar/upload-url")
    public ResponseEntity<UploadUrlResponse> getAvatarUploadUrl(@Valid @RequestBody UploadUrlRequest request) {
        UploadUrlResponse response = s3Service.generatePresignedUploadUrl(
            "avatars",
            request.getContentType(),
            request.getContentLength()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<UserDto> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        String userId = organizationContext.getCurrentUser().getId().toString();
        UserDto updatedUser = profileService.updateProfileImage(userId, request.getProfileImageUrl());
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> deleteAvatar() {
        String userId = organizationContext.getCurrentUser().getId().toString();
        profileService.deleteProfileImage(userId);
        return ResponseEntity.noContent().build();
    }

        @GetMapping("/me")

        public ResponseEntity<JsonNode> getCurrentUserProfile() {

            UUID userId = organizationContext.getCurrentUser().getId();

            JsonNode profile = profileService.getFullProfile(userId);

            return ResponseEntity.ok(profile);

        }

    

        @PutMapping("/me")

        public ResponseEntity<JsonNode> updateFullProfile(@RequestBody JsonNode profile) {

            UUID userId = organizationContext.getCurrentUser().getId();

            JsonNode updatedProfile = profileService.updateFullProfile(userId, profile);

            return ResponseEntity.ok(updatedProfile);

        }

    @PostMapping("/me/resumes/upload-url")
    public ResponseEntity<UploadUrlResponse> getResumeUploadUrl(@Valid @RequestBody ResumeUploadRequest request) {
        UUID userId = organizationContext.getCurrentUser().getId();
        UploadUrlResponse response = profileService.getResumeUploadUrl(
            userId,
            request.getContentType(),
            request.getContentLength(),
            request.getFileName()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/me/resumes")
    public ResponseEntity<ResumeDto> addResumeToProfile(@Valid @RequestBody ResumeDto request) {
        UUID userId = organizationContext.getCurrentUser().getId();
        ResumeDto newResume = profileService.addResumeToProfile(
            userId,
            request.getUrl(),
            request.getFilename()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(newResume);
    }

    @DeleteMapping("/me/resumes/{resumeId}")
    public ResponseEntity<Void> deleteResume(@PathVariable UUID resumeId) {
        UUID userId = organizationContext.getCurrentUser().getId();
        profileService.deleteResume(userId, resumeId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/resumes")
    public ResponseEntity<List<ResumeDto>> getResumes() {
        UUID userId = organizationContext.getCurrentUser().getId();
        List<ResumeDto> resumes = profileService.getResumes(userId);
        return ResponseEntity.ok(resumes);
    }

    @PostMapping("/me/autofill-resume")
    public ResponseEntity<JsonNode> autofillProfileFromResume(@Valid @RequestBody ResumeDto request) {
        UUID userId = organizationContext.getCurrentUser().getId();
        JsonNode updatedProfile = profileService.autofillProfileFromResume(userId, request.getUrl());
        return ResponseEntity.ok(updatedProfile);
    }
}