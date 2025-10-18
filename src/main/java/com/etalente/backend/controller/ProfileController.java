package com.etalente.backend.controller;

import com.etalente.backend.dto.*;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.ProfileService;
import com.etalente.backend.service.S3Service;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<UserDto> getCurrentUserProfile() {
        String userId = organizationContext.getCurrentUser().getId().toString();
        UserDto user = profileService.getUserProfile(userId);
        return ResponseEntity.ok(user);
    }
}