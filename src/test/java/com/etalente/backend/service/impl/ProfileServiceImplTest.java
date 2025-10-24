package com.etalente.backend.service.impl;

import com.etalente.backend.dto.ResumeDto;
import com.etalente.backend.dto.UploadUrlResponse;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.integration.documentparser.DocumentParserClient;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private S3Service s3Service;
    private ObjectMapper objectMapper;
    @Mock
    private DocumentParserClient documentParserClient;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private UUID userId;
    private User candidateUser;
    private User nonCandidateUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        userId = UUID.randomUUID();
        candidateUser = new User();
        candidateUser.setId(userId);
        candidateUser.setRole(Role.CANDIDATE);
        candidateUser.setProfile(objectMapper.createObjectNode()); // Initialize profile

        nonCandidateUser = new User();
        nonCandidateUser.setId(UUID.randomUUID());
        nonCandidateUser.setRole(Role.HIRING_MANAGER);
    }

    @Nested
    @DisplayName("Resume Upload URL Generation")
    class ResumeUploadUrlGenerationTests {

        @Test
        void getResumeUploadUrl_shouldReturnPresignedUrl_forCandidate() {
            // Given
            String contentType = "application/pdf";
            long contentLength = 1024L;
            String fileName = "my_resume.pdf";
            UploadUrlResponse mockResponse = new UploadUrlResponse("http://presigned.url", "http://public.url", "resumes/key");

            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
            when(s3Service.generatePresignedUploadUrl(anyString(), anyString(), anyLong(), anyString()))
                    .thenReturn(mockResponse);

            // When
            UploadUrlResponse response = profileService.getResumeUploadUrl(userId, contentType, contentLength, fileName);

            // Then
            assertThat(response).isEqualTo(mockResponse);
            verify(s3Service, times(1)).generatePresignedUploadUrl(eq("resumes"), eq(contentType), eq(contentLength), anyString());
        }

        @Test
        void getResumeUploadUrl_shouldThrowBadRequest_forNonCandidate() {
            // Given
            when(userRepository.findById(nonCandidateUser.getId())).thenReturn(Optional.of(nonCandidateUser));

            // When & Then
            assertThatThrownBy(() -> profileService.getResumeUploadUrl(nonCandidateUser.getId(), "application/pdf", 1024L, "resume.pdf"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only candidates can upload resumes");
        }

        @Test
        void getResumeUploadUrl_shouldThrowBadRequest_ifMaxResumesExceeded() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
            ArrayNode resumesNode = new ObjectMapper().createArrayNode();
            for (int i = 0; i < 3; i++) {
                resumesNode.add(new ObjectMapper().createObjectNode());
            }
            candidateUser.setResumes(resumesNode);

            // When & Then
            assertThatThrownBy(() -> profileService.getResumeUploadUrl(userId, "application/pdf", 1024L, "resume.pdf"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Maximum of 3 resumes allowed");
        }
    }

    @Nested
    @DisplayName("Add Resume to Profile")
    class AddResumeToProfileTests {

        @Test
        void addResumeToProfile_shouldAddResume_forCandidate() {
            // Given
            String resumeUrl = "http://s3.url/resume.pdf";
            String fileName = "my_resume.pdf";
            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
            when(objectMapper.valueToTree(any(List.class))).thenReturn(new ObjectMapper().createArrayNode());
            when(userRepository.save(any(User.class))).thenReturn(candidateUser);

            // When
            ResumeDto result = profileService.addResumeToProfile(userId, resumeUrl, fileName);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUrl()).isEqualTo(resumeUrl);
            assertThat(result.getFilename()).isEqualTo(fileName);
            verify(userRepository, times(1)).save(candidateUser);
        }

        @Test
        void addResumeToProfile_shouldThrowBadRequest_forNonCandidate() {
            // Given
            when(userRepository.findById(nonCandidateUser.getId())).thenReturn(Optional.of(nonCandidateUser));

            // When & Then
            assertThatThrownBy(() -> profileService.addResumeToProfile(nonCandidateUser.getId(), "http://url", "file.pdf"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only candidates can add resumes to their profile");
        }

        @Test
        void addResumeToProfile_shouldThrowBadRequest_ifMaxResumesExceeded() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
            ArrayNode resumesNode = new ObjectMapper().createArrayNode();
            for (int i = 0; i < 3; i++) {
                resumesNode.add(new ObjectMapper().createObjectNode());
            }
            candidateUser.setResumes(resumesNode);

            // When & Then
            assertThatThrownBy(() -> profileService.addResumeToProfile(userId, "http://url", "file.pdf"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Maximum of 3 resumes allowed");
        }
    }

    @Nested
    @DisplayName("Delete Resume from Profile")
    class DeleteResumeTests {

        private UUID resumeIdToDelete;
        private String resumeUrlToDelete;

        @BeforeEach
        void setup() {
            resumeIdToDelete = UUID.randomUUID();
            resumeUrlToDelete = "http://s3.url/resume_to_delete.pdf";
            ResumeDto existingResume = new ResumeDto(resumeIdToDelete, resumeUrlToDelete, "delete.pdf", LocalDateTime.now());
            candidateUser.setResumes(new ObjectMapper().valueToTree(List.of(existingResume)));
        }

        @Test
        void deleteResume_shouldDeleteResume_forCandidate() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
            doNothing().when(s3Service).deleteFile(anyString());
            when(objectMapper.valueToTree(any(List.class))).thenReturn(new ObjectMapper().createArrayNode());
            when(userRepository.save(any(User.class))).thenReturn(candidateUser);

            // When
            profileService.deleteResume(userId, resumeIdToDelete);

            // Then
            verify(s3Service, times(1)).deleteFile(resumeUrlToDelete);
            verify(userRepository, times(1)).save(candidateUser);
            assertThat(candidateUser.getResumes()).isNotNull(); // Should be empty array, not null
        }

        @Test
        void deleteResume_shouldThrowBadRequest_forNonCandidate() {
            // Given
            when(userRepository.findById(nonCandidateUser.getId())).thenReturn(Optional.of(nonCandidateUser));

            // When & Then
            assertThatThrownBy(() -> profileService.deleteResume(nonCandidateUser.getId(), UUID.randomUUID()))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only candidates can delete resumes from their profile");
        }

        @Test
        void deleteResume_shouldThrowResourceNotFound_ifResumeNotFound() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));

            // When & Then
            assertThatThrownBy(() -> profileService.deleteResume(userId, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Resume not found");
        }
    }

    @Nested
    @DisplayName("Get Resumes from Profile")
    class GetResumesTests {

        @Test
        void getResumes_shouldReturnListOfResumes_forCandidate() {
            // Given
            ResumeDto resume1 = new ResumeDto(UUID.randomUUID(), "http://s3.url/resume1.pdf", "resume1.pdf", LocalDateTime.now());
            ResumeDto resume2 = new ResumeDto(UUID.randomUUID(), "http://s3.url/resume2.pdf", "resume2.pdf", LocalDateTime.now());
            candidateUser.setResumes(new ObjectMapper().valueToTree(List.of(resume1, resume2)));
            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
            when(objectMapper.convertValue(any(JsonNode.class), eq(ResumeDto.class)))
                    .thenReturn(resume1, resume2); // Mock conversion for each element

            // When
            List<ResumeDto> result = profileService.getResumes(userId);

            // Then
            assertThat(result).isNotNull().hasSize(2);
            assertThat(result.get(0).getFilename()).isEqualTo(resume1.getFilename());
            assertThat(result.get(1).getFilename()).isEqualTo(resume2.getFilename());
        }

        @Test
        void getResumes_shouldReturnEmptyList_ifNoResumes() {
            // Given
            candidateUser.setResumes(null);
            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));

            // When
            List<ResumeDto> result = profileService.getResumes(userId);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Autofill Profile from Resume")
    class AutofillProfileFromResumeTests {

        @Test
        void autofillProfileFromResume_shouldUpdateProfile_forCandidate() {
            // Given
            String resumeS3Url = "http://s3.url/resume.pdf";
            ObjectNode extractedData = new ObjectMapper().createObjectNode();
            extractedData.put("education", "BSc Computer Science");
            extractedData.put("experienceYears", 5);
            extractedData.put("firstName", "Extracted"); // Should be ignored
            extractedData.put("lastName", "User"); // Should be ignored

            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
            when(documentParserClient.extractResume(resumeS3Url)).thenReturn(extractedData);
            when(objectMapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode()); // For initial profile
            when(userRepository.save(any(User.class))).thenReturn(candidateUser);

            // When
            JsonNode updatedProfile = profileService.autofillProfileFromResume(userId, resumeS3Url);

            // Then
            assertThat(updatedProfile).isNotNull();
            assertThat(updatedProfile.has("education")).isTrue();
            assertThat(updatedProfile.get("education").asText()).isEqualTo("BSc Computer Science");
            assertThat(updatedProfile.has("experienceYears")).isTrue();
            assertThat(updatedProfile.get("experienceYears").asInt()).isEqualTo(5);
            assertThat(updatedProfile.has("firstName")).isFalse(); // Should not be merged
            assertThat(updatedProfile.has("lastName")).isFalse(); // Should not be merged
            verify(userRepository, times(1)).save(candidateUser);
            assertThat(candidateUser.isProfileComplete()).isTrue();
        }

        @Test
        void autofillProfileFromResume_shouldThrowBadRequest_forNonCandidate() {
            // Given
            when(userRepository.findById(nonCandidateUser.getId())).thenReturn(Optional.of(nonCandidateUser));

            // When & Then
            assertThatThrownBy(() -> profileService.autofillProfileFromResume(nonCandidateUser.getId(), "http://url"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only candidates can autofill their profile");
        }

        @Test
        void autofillProfileFromResume_shouldHandleEmptyExtractedData() {
            // Given
            String resumeS3Url = "http://s3.url/empty.pdf";
            when(userRepository.findById(userId)).thenReturn(Optional.of(candidateUser));
            when(documentParserClient.extractResume(resumeS3Url)).thenReturn(new ObjectMapper().createObjectNode());
            when(objectMapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
            when(userRepository.save(any(User.class))).thenReturn(candidateUser);

            // When
            JsonNode updatedProfile = profileService.autofillProfileFromResume(userId, resumeS3Url);

            // Then
            assertThat(updatedProfile).isNotNull();
            assertThat(updatedProfile.isEmpty()).isTrue();
            verify(userRepository, times(1)).save(candidateUser);
            assertThat(candidateUser.isProfileComplete()).isTrue();
        }
    }
}
