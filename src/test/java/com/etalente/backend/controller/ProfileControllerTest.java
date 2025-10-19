package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.dto.UpdateAvatarRequest;
import com.etalente.backend.dto.UploadUrlRequest;
import com.etalente.backend.dto.UploadUrlResponse;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.S3Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ProfileControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Service s3Service;

    private User user;
    private String userJwt;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();
        user = testHelper.createUser("test@example.com", Role.CANDIDATE);
        userJwt = testHelper.generateJwtForUser(user);
    }

    @Test
    void updateProfile_shouldUpdateName() throws Exception {
        UUID userId = user.getId();

        Map<String, String> profileData = new HashMap<>();
        profileData.put("firstName", "John");
        profileData.put("lastName", "Doe");

        mockMvc.perform(put("/api/profile/update")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileData)))
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo("John");
        assertThat(updatedUser.getLastName()).isEqualTo("Doe");
    }

    @Test
    void completeProfile_shouldUpdateUserAndReturnNewToken() throws Exception {
        // 1. Create a new user who is marked as isNewUser=true
        User newUser = testHelper.createUser("newuser@example.com", Role.CANDIDATE);
        newUser.setNewUser(true);
        userRepository.save(newUser);
        assertThat(newUser.isNewUser()).isTrue();
        String initialToken = testHelper.generateJwtForUser(newUser);

        // 2. Prepare the profile completion request
        CompleteProfileRequest request = new CompleteProfileRequest();
        request.setFirstName("Jane");
        request.setLastName("Doe");

        // 3. Call the new endpoint
        mockMvc.perform(put("/api/profile/complete")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.isNewUser").value(false))
                .andExpect(jsonPath("$.user.firstName").value("Jane"));

        // 4. Verify the user is no longer marked as new in the database
        User updatedUser = userRepository.findById(newUser.getId()).orElseThrow();
        assertThat(updatedUser.isNewUser()).isFalse();
        assertThat(updatedUser.getFirstName()).isEqualTo("Jane");
    }

    @Test
    void authenticatedUserCanGetAvatarUploadUrl() throws Exception {
        UploadUrlRequest request = new UploadUrlRequest("image/jpeg", 2048L);
        UploadUrlResponse response = new UploadUrlResponse("https://s3.presigned.url/upload", "https://s3.final.url/avatar.jpg", "avatars/some-key.jpg");
        when(s3Service.generatePresignedUploadUrl(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/profile/me/avatar/upload-url")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value(response.getUploadUrl()))
                .andExpect(jsonPath("$.fileUrl").value(response.getFileUrl()));
    }

    @Test
    void unauthenticatedUserCannotGetAvatarUploadUrl() throws Exception {
        UploadUrlRequest request = new UploadUrlRequest("image/jpeg", 2048L);

        mockMvc.perform(post("/api/profile/me/avatar/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserCanUpdateAvatar() throws Exception {
        String newAvatarUrl = "https://s3.final.url/new-avatar.jpg";
        UpdateAvatarRequest request = new UpdateAvatarRequest(newAvatarUrl);

        mockMvc.perform(put("/api/profile/me/avatar")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(newAvatarUrl));
    }

    @Test
    void authenticatedUserCanDeleteAvatar() throws Exception {
        mockMvc.perform(delete("/api/profile/me/avatar")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void getFullProfile_returnsCorrectProfile() throws Exception {
        // Given
        String profileJson = """
                {
                  "basics": {
                    "name": "John Doe",
                    "email": "test@example.com"
                  }
                }
                """;
        JsonNode profile = objectMapper.readTree(profileJson);
        user.setProfile(profile);
        userRepository.save(user);

        // When & Then
        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basics.email").value("test@example.com"));
    }

    @Test
    void updateProfile_savesAndReturnsProfile() throws Exception {
        String profileJson = """
                {
                  "basics": { "name": "John Doe" },
                  "work": [{ "name": "Company" }]
                }
                """;

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(profileJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basics.name").value("John Doe"));
    }

    @Test
    void getProfile_whenNoProfileExists_returnsEmptyObject() throws Exception {
        mockMvc.perform(get("/api/profile/me")
                        .header("Authorization", "Bearer " + userJwt))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    @Test
    void updateProfile_withInvalidJson_returnsBadRequest() throws Exception {
        String invalidJson = """
                {
                  "basics": { "name": "John Doe" }
                  "work": [{ "name": "Company" }]
                }
                """;

        mockMvc.perform(put("/api/profile/me")
                        .header("Authorization", "Bearer " + userJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
