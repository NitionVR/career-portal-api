package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.ResumeDto;
import com.etalente.backend.dto.ResumeUploadRequest;
import com.etalente.backend.dto.UploadUrlResponse;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.service.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ProfileResumeControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestHelper testHelper;

    @MockBean
    private S3Service s3Service;

    private User candidateUser;
    private User hiringManagerUser;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();
        candidateUser = testHelper.createUser("candidate@example.com", Role.CANDIDATE);
        hiringManagerUser = testHelper.createUser("hm@example.com", Role.HIRING_MANAGER);
    }

    @Test
    @DisplayName("POST /me/resumes/upload-url should return a presigned URL for candidate")
    void getResumeUploadUrl_candidate_shouldReturnPresignedUrl() throws Exception {
        authenticateAs(candidateUser.getId());

        ResumeUploadRequest request = new ResumeUploadRequest("application/pdf", 1024L, "my_resume.pdf");
        UploadUrlResponse mockResponse = new UploadUrlResponse("http://presigned.url/resume", "http://public.url/resume", "resumes/key");

        when(s3Service.generatePresignedUploadUrl(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/profile/me/resumes/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl", is("http://presigned.url/resume")))
                .andExpect(jsonPath("$.fileUrl", is("http://public.url/resume")));

        verify(s3Service, times(1)).generatePresignedUploadUrl(eq("resumes"), eq("application/pdf"), eq(1024L), anyString());
    }

    @Test
    @DisplayName("POST /me/resumes/upload-url should return 400 if candidate already has max resumes")
    void getResumeUploadUrl_candidate_maxResumes_shouldReturnBadRequest() throws Exception {
        authenticateAs(candidateUser.getId());

        // Add 3 mock resumes to the user
        User userWithResumes = userRepository.findById(candidateUser.getId()).orElseThrow();
        ArrayNode resumesNode = objectMapper.createArrayNode();
        for (int i = 0; i < 3; i++) {
            ObjectNode resume = objectMapper.createObjectNode();
            resume.put("id", UUID.randomUUID().toString());
            resume.put("url", "http://resume" + i + ".url");
            resume.put("filename", "resume" + i + ".pdf");
            resume.put("uploadDate", LocalDateTime.now().toString());
            resumesNode.add(resume);
        }
        userWithResumes.setResumes(resumesNode);
        userRepository.save(userWithResumes);

        ResumeUploadRequest request = new ResumeUploadRequest("application/pdf", 1024L, "my_resume.pdf");

        mockMvc.perform(post("/api/profile/me/resumes/upload-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Maximum of 3 resumes allowed")));
    }

    @Test
    @DisplayName("POST /me/resumes should add a resume to candidate profile")
    void addResumeToProfile_shouldAddResume() throws Exception {
        authenticateAs(candidateUser.getId());

        ResumeDto request = new ResumeDto(null, "http://public.url/new_resume.pdf", "new_resume.pdf", null);

        mockMvc.perform(post("/api/profile/me/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url", is("http://public.url/new_resume.pdf")))
                .andExpect(jsonPath("$.filename", is("new_resume.pdf")));

        User updatedUser = userRepository.findById(candidateUser.getId()).orElseThrow();
        ArrayNode resumes = (ArrayNode) updatedUser.getResumes();
        assertThat(resumes.size()).isEqualTo(1);
        assertThat(resumes.get(0).get("url").asText()).isEqualTo("http://public.url/new_resume.pdf");
    }

    @Test
    @DisplayName("DELETE /me/resumes/{resumeId} should delete a resume from candidate profile and S3")
    void deleteResume_shouldDeleteResume() throws Exception {
        authenticateAs(candidateUser.getId());

        // Add a resume first
        ResumeDto resumeToAdd = new ResumeDto(UUID.randomUUID(), "http://public.url/to_delete.pdf", "to_delete.pdf", LocalDateTime.now());
        User userWithResume = userRepository.findById(candidateUser.getId()).orElseThrow();
        userWithResume.setResumes(objectMapper.valueToTree(List.of(resumeToAdd)));
        userRepository.save(userWithResume);

        doNothing().when(s3Service).deleteFile(anyString());

        mockMvc.perform(delete("/api/profile/me/resumes/{resumeId}", resumeToAdd.getId()))
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(candidateUser.getId()).orElseThrow();
        ArrayNode resumes = (ArrayNode) updatedUser.getResumes();
        assertThat(resumes.size()).isEqualTo(0);
        verify(s3Service, times(1)).deleteFile("http://public.url/to_delete.pdf");
    }

    @Test
    @DisplayName("GET /me/resumes should return list of resumes for candidate")
    void getResumes_shouldReturnResumes() throws Exception {
        authenticateAs(candidateUser.getId());

        // Add a resume first
        ResumeDto resume1 = new ResumeDto(UUID.randomUUID(), "http://public.url/resume1.pdf", "resume1.pdf", LocalDateTime.now());
        ResumeDto resume2 = new ResumeDto(UUID.randomUUID(), "http://public.url/resume2.pdf", "resume2.pdf", LocalDateTime.now());
        User userWithResumes = userRepository.findById(candidateUser.getId()).orElseThrow();
        userWithResumes.setResumes(objectMapper.valueToTree(List.of(resume1, resume2)));
        userRepository.save(userWithResumes);

        mockMvc.perform(get("/api/profile/me/resumes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(2)))
                .andExpect(jsonPath("$.[0].filename", is("resume1.pdf")))
                .andExpect(jsonPath("$.[1].filename", is("resume2.pdf")));
    }

    // @Test
    // @DisplayName("Non-candidate user should be forbidden from resume operations")
    // void resumeOperations_nonCandidate_shouldBeForbidden() throws Exception {
    //     authenticateAs(hiringManagerUser.getId());

    //     ResumeUploadRequest uploadRequest = new ResumeUploadRequest("application/pdf", 1024L, "my_resume.pdf");
    //     mockMvc.perform(post("/api/profile/me/resumes/upload-url")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(uploadRequest)))
    //             .andExpect(status().isBadRequest());

    //     ResumeDto addRequest = new ResumeDto(null, "http://public.url/new_resume.pdf", "new_resume.pdf", null);
    //     mockMvc.perform(post("/api/profile/me/resumes")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .content(objectMapper.writeValueAsString(addRequest)))
    //             .andExpect(status().isBadRequest());

    //     mockMvc.perform(delete("/api/profile/me/resumes/{resumeId}", UUID.randomUUID()))
    //             .andExpect(status().isBadRequest());

    //     mockMvc.perform(get("/api/profile/me/resumes"))
    //             .andExpect(status().isBadRequest());
    // }
}
