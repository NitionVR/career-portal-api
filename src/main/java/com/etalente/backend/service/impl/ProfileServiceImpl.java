package com.etalente.backend.service.impl;

import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.dto.ResumeDto;
import com.etalente.backend.dto.UploadUrlResponse;
import com.etalente.backend.dto.UserDto;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.ProfileService;
import com.etalente.backend.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);
    private static final int MAX_RESUMES = 3;

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

    @Override
    public UploadUrlResponse getResumeUploadUrl(UUID userId, String contentType, long contentLength, String fileName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CANDIDATE) {
            throw new BadRequestException("Only candidates can upload resumes");
        }

        List<ResumeDto> currentResumes = getResumes(userId);
        if (currentResumes.size() >= MAX_RESUMES) {
            throw new BadRequestException(String.format("Maximum of %d resumes allowed", MAX_RESUMES));
        }

        // Generate a unique key for the resume
        String key = "resumes/" + userId.toString() + "/" + UUID.randomUUID().toString() + "-" + fileName;

        return s3Service.generatePresignedUploadUrl("resumes", contentType, contentLength, key);
    }

    @Override
    public ResumeDto addResumeToProfile(UUID userId, String resumeUrl, String fileName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CANDIDATE) {
            throw new BadRequestException("Only candidates can add resumes to their profile");
        }

        List<ResumeDto> currentResumes = getResumes(userId);
        if (currentResumes.size() >= MAX_RESUMES) {
            throw new BadRequestException(String.format("Maximum of %d resumes allowed", MAX_RESUMES));
        }

        ResumeDto newResume = new ResumeDto(UUID.randomUUID(), resumeUrl, fileName, LocalDateTime.now());
        currentResumes.add(newResume);

        user.setResumes(objectMapper.valueToTree(currentResumes));
        userRepository.save(user);

        logger.info("Resume added to profile for user {}: {}", userId, fileName);
        return newResume;
    }

    @Override
    public void deleteResume(UUID userId, UUID resumeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CANDIDATE) {
            throw new BadRequestException("Only candidates can delete resumes from their profile");
        }

        List<ResumeDto> currentResumes = getResumes(userId);
        Optional<ResumeDto> resumeToDelete = currentResumes.stream()
                .filter(r -> r.getId().equals(resumeId))
                .findFirst();

        if (resumeToDelete.isEmpty()) {
            throw new ResourceNotFoundException("Resume not found with id: " + resumeId);
        }

        // Delete from S3
        s3Service.deleteFile(resumeToDelete.get().getUrl());

        // Remove from list and update user profile
        List<ResumeDto> updatedResumes = currentResumes.stream()
                .filter(r -> !r.getId().equals(resumeId))
                .collect(Collectors.toList());

        user.setResumes(objectMapper.valueToTree(updatedResumes));
        userRepository.save(user);

        logger.info("Resume {} deleted for user {}", resumeId, userId);
    }

    @Override
    public List<ResumeDto> getResumes(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getResumes() == null || user.getResumes().isNull() || user.getResumes().isEmpty()) {
            return new ArrayList<>();
        }

        return StreamSupport.stream(user.getResumes().spliterator(), false)
                .map(jsonNode -> objectMapper.convertValue(jsonNode, ResumeDto.class))
                .collect(Collectors.toList());
    }
}