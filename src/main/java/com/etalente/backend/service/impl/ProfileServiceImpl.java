package com.etalente.backend.service.impl;

import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.dto.UserDto;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.ProfileService;
import com.etalente.backend.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    public ProfileServiceImpl(UserRepository userRepository, JwtService jwtService, S3Service s3Service, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.s3Service = s3Service;
        this.objectMapper = objectMapper;
    }

    @Override
    public void updateProfile(UUID userId, Map<String, String> profileData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        profileData.forEach((key, value) -> {
            switch (key) {
                case "firstName":
                    user.setFirstName(value);
                    break;
                case "lastName":
                    user.setLastName(value);
                    break;
                case "contactNumber":
                    user.setContactNumber(value);
                    break;
                case "summary":
                    user.setSummary(value);
                    break;
                // Add other fields as necessary
            }
        });

        user.setProfileComplete(true);
        userRepository.save(user);
    }

    @Override
    public VerifyTokenResponse completeProfile(UUID userId, CompleteProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update user details
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (user.getRole() == Role.HIRING_MANAGER && request.getCompanyName() != null) {
            user.setCompanyName(request.getCompanyName());
        }
        user.setProfileComplete(true);
        user.setNewUser(false); // Mark profile as completed

        User updatedUser = userRepository.save(user);

        // Generate a new token with isNewUser=false
        String newToken = jwtService.generateToken(
                updatedUser.getId().toString(),
                updatedUser.getEmail(),
                updatedUser.getRole().name(),
                false, // isNewUser is now false
                updatedUser.getUsername()
        );

        VerifyTokenResponse.UserDto userDto = new VerifyTokenResponse.UserDto(
                updatedUser.getId().toString(),
                updatedUser.getEmail(),
                updatedUser.getRole().name(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                false, // isNewUser is now false
                null // profileImageUrl is not available here
        );

        long expiresIn = jwtService.getTimeUntilExpiration(newToken);

        return new VerifyTokenResponse(newToken, userDto, expiresIn);
    }

    @Override
    public UserDto updateProfileImage(String userId, String profileImageUrl) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Delete old image if exists
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            try {
                s3Service.deleteFile(user.getProfileImageUrl());
            } catch (Exception e) {
                logger.error("Failed to delete old profile image: {}", user.getProfileImageUrl(), e);
            }
        }

        user.setProfileImageUrl(profileImageUrl);
        user = userRepository.save(user);

        logger.info("Profile image updated for user: {}", userId);

        return UserDto.fromEntity(user);
    }

    @Override
    public void deleteProfileImage(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getProfileImageUrl() != null) {
            s3Service.deleteFile(user.getProfileImageUrl());
            user.setProfileImageUrl(null);
            userRepository.save(user);

            logger.info("Profile image deleted for user: {}", userId);
        }
    }

    @Override
        public UserDto getUserProfile(String userId) {
            User user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    
            return UserDto.fromEntity(user);
        }
    
        @Override
        public JsonNode getFullProfile(UUID userId) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            if (user.getProfile() == null) {
                return objectMapper.createObjectNode();
            }
            return user.getProfile();
        }
    
        @Override
        public JsonNode updateFullProfile(UUID userId, JsonNode profile) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            user.setProfile(profile);
            userRepository.save(user);
            return user.getProfile();
        }
    }
    