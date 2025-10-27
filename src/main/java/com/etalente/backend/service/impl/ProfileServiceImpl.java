package com.etalente.backend.service.impl;

import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.dto.ResumeDto;
import com.etalente.backend.dto.UploadUrlResponse;
import com.etalente.backend.dto.UserDto;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.integration.documentparser.DocumentParserClient;
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
    private final DocumentParserClient documentParserClient;

    public ProfileServiceImpl(UserRepository userRepository, JwtService jwtService, S3Service s3Service, ObjectMapper objectMapper, DocumentParserClient documentParserClient) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.s3Service = s3Service;
        this.objectMapper = objectMapper;
        this.documentParserClient = documentParserClient;
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

    @Override
    public JsonNode autofillProfileFromResume(UUID userId, String resumeS3Url) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getRole() != Role.CANDIDATE) {
            throw new BadRequestException("Only candidates can autofill their profile from a resume");
        }

        // Call document parser to extract resume data
        JsonNode documentParserResponse = documentParserClient.extractResume(resumeS3Url);

        if (documentParserResponse == null || documentParserResponse.isNull() || documentParserResponse.isEmpty()) {
            logger.warn("Document parser returned no extracted profile data for resume: {}", resumeS3Url);
            return (user.getProfile() != null && !user.getProfile().isNull())
                    ? user.getProfile() : objectMapper.createObjectNode();
        }

        // Extract ONLY the JSON Resume schema data from extracted_data
        JsonNode extractedData = documentParserResponse.get("extracted_data");
        if (extractedData == null || extractedData.isNull()) {
            logger.warn("No extracted_data found in document parser response");
            return (user.getProfile() != null && !user.getProfile().isNull())
                    ? user.getProfile() : objectMapper.createObjectNode();
        }

        // Get current profile or create new one
        ObjectNode currentProfile = (user.getProfile() != null && !user.getProfile().isNull())
                ? (ObjectNode) user.getProfile().deepCopy() : objectMapper.createObjectNode();

        // Merge root fields
        mergeJsonNodes(currentProfile, extractedData);

        // Merge each section with deduplication
        mergeSection(currentProfile, extractedData, "basics");
        mergeArraySection(currentProfile, extractedData, "work");
        mergeArraySection(currentProfile, extractedData, "education");
        mergeArraySection(currentProfile, extractedData, "skills");
        mergeArraySection(currentProfile, extractedData, "projects");
        mergeArraySection(currentProfile, extractedData, "awards");
        mergeArraySection(currentProfile, extractedData, "certificates");
        mergeArraySection(currentProfile, extractedData, "interests");
        mergeArraySection(currentProfile, extractedData, "languages");
        mergeArraySection(currentProfile, extractedData, "publications");
        mergeArraySection(currentProfile, extractedData, "references");
        mergeArraySection(currentProfile, extractedData, "volunteer");

        // Preserve firstName and lastName from user entity in basics
        if (currentProfile.has("basics") && currentProfile.get("basics").isObject()) {
            ObjectNode basics = (ObjectNode) currentProfile.get("basics");
            if (user.getFirstName() != null) {
                String fullName = user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
                basics.put("name", fullName);
            }
            if (user.getEmail() != null) {
                basics.put("email", user.getEmail());
            }
        }

        user.setProfile(currentProfile);
        user.setProfileComplete(true);
        userRepository.save(user);

        logger.info("Profile autofilled for user {} from resume: {}", userId, resumeS3Url);
        return currentProfile; // Return the full profile, not just basics
    }

    // Helper method to merge a single section (like basics)
    private void mergeSection(ObjectNode target, JsonNode source, String sectionName) {
        JsonNode sourceSection = source.get(sectionName);
        if (sourceSection == null || sourceSection.isNull()) {
            return;
        }

        if (target.has(sectionName) && target.get(sectionName).isObject() && sourceSection.isObject()) {
            mergeJsonNodes((ObjectNode) target.get(sectionName), sourceSection);
        } else {
            target.set(sectionName, sourceSection.deepCopy());
        }
    }

    // Helper method to merge and deduplicate array sections
    private void mergeArraySection(ObjectNode target, JsonNode source, String sectionName) {
        JsonNode sourceArray = source.get(sectionName);
        if (sourceArray == null || !sourceArray.isArray() || sourceArray.isEmpty()) {
            return;
        }

        ArrayNode targetArray;
        if (target.has(sectionName) && target.get(sectionName).isArray()) {
            targetArray = (ArrayNode) target.get(sectionName);
        } else {
            targetArray = objectMapper.createArrayNode();
            target.set(sectionName, targetArray);
        }

        // Deduplicate by converting to Set using JSON string representation
        List<JsonNode> uniqueItems = new ArrayList<>();
        List<String> seenItems = new ArrayList<>();

        // Add existing items
        for (JsonNode item : targetArray) {
            String itemStr = item.toString();
            if (!seenItems.contains(itemStr)) {
                uniqueItems.add(item);
                seenItems.add(itemStr);
            }
        }

        // Add new items from source, skipping duplicates
        for (JsonNode item : sourceArray) {
            String itemStr = item.toString();
            if (!seenItems.contains(itemStr)) {
                uniqueItems.add(item);
                seenItems.add(itemStr);
            }
        }

        // Rebuild array with unique items
        targetArray.removeAll();
        uniqueItems.forEach(targetArray::add);
    }

    // Update the existing mergeJsonNodes to handle profiles properly
    private void mergeJsonNodes(ObjectNode mainNode, JsonNode updateNode) {
        updateNode.fields().forEachRemaining(entry -> {
            String fieldName = entry.getKey();
            JsonNode updateValue = entry.getValue();

            // Skip these fields as they come from user entity
            if ("name".equals(fieldName) || "email".equals(fieldName)) {
                return;
            }

            if (mainNode.has(fieldName)) {
                JsonNode mainValue = mainNode.get(fieldName);

                if (mainValue.isObject() && updateValue.isObject()) {
                    mergeJsonNodes((ObjectNode) mainValue, updateValue);
                } else if (mainValue.isArray() && updateValue.isArray()) {
                    // For arrays in basics (like profiles), deduplicate
                    ArrayNode mainArray = (ArrayNode) mainValue;
                    ArrayNode updateArray = (ArrayNode) updateValue;

                    List<String> seenItems = new ArrayList<>();
                    for (JsonNode item : mainArray) {
                        seenItems.add(item.toString());
                    }

                    for (JsonNode item : updateArray) {
                        String itemStr = item.toString();
                        if (!seenItems.contains(itemStr)) {
                            mainArray.add(item);
                            seenItems.add(itemStr);
                        }
                    }
                } else {
                    mainNode.set(fieldName, updateValue);
                }
            } else {
                mainNode.set(fieldName, updateValue.deepCopy());
            }
        });
    }
}