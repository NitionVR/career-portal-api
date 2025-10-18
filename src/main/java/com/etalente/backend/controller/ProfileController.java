package com.etalente.backend.controller;

import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PutMapping("/update")
    public ResponseEntity<Void> updateProfile(@RequestBody Map<String, String> profileData) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(authentication.getName());
        profileService.updateProfile(userId, profileData);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/complete")
    public ResponseEntity<VerifyTokenResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(authentication.getName());
        VerifyTokenResponse response = profileService.completeProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}