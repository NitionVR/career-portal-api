package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.UpdateCompanyLogoRequest;
import com.etalente.backend.dto.UploadUrlRequest;
import com.etalente.backend.dto.UploadUrlResponse;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.service.S3Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
public class OrganizationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestHelper testHelper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @MockBean
    private S3Service s3Service;

    private User hiringManager;
    private String hiringManagerJwt;
    private Organization organization;

    @BeforeEach
    void setUp() {
        testHelper.cleanupDatabase();
        hiringManager = testHelper.createUser("hm@example.com", Role.HIRING_MANAGER);
        hiringManagerJwt = testHelper.generateJwtForUser(hiringManager);
        organization = hiringManager.getOrganization();
    }

    @Test
    void hiringManagerCanGetLogoUploadUrl() throws Exception {
        UploadUrlRequest request = new UploadUrlRequest("image/png", 1024L);
        UploadUrlResponse response = new UploadUrlResponse("https://s3.presigned.url/upload", "https://s3.final.url/logo.png", "avatars/some-key.png");
        when(s3Service.generatePresignedUploadUrl(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/organization/logo/upload-url")
                        .header("Authorization", "Bearer " + hiringManagerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadUrl").value(response.getUploadUrl()))
                .andExpect(jsonPath("$.fileUrl").value(response.getFileUrl()));
    }

    @Test
    void recruiterCannotGetLogoUploadUrl() throws Exception {
        User recruiter = testHelper.createUser("recruiter@example.com", Role.RECRUITER);
        String recruiterJwt = testHelper.generateJwtForUser(recruiter);
        UploadUrlRequest request = new UploadUrlRequest("image/png", 1024L);

        mockMvc.perform(post("/api/organization/logo/upload-url")
                        .header("Authorization", "Bearer " + recruiterJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void hiringManagerCanUpdateCompanyLogo() throws Exception {
        String newLogoUrl = "https://s3.final.url/new-logo.png";
        UpdateCompanyLogoRequest request = new UpdateCompanyLogoRequest(newLogoUrl);

        mockMvc.perform(put("/api/organization/logo")
                        .header("Authorization", "Bearer " + hiringManagerJwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyLogoUrl").value(newLogoUrl));
    }

    @Test
    void hiringManagerCanDeleteCompanyLogo() throws Exception {
        organization.setCompanyLogoUrl("https://s3.final.url/logo.png");
        organizationRepository.save(organization);

        mockMvc.perform(delete("/api/organization/logo")
                        .header("Authorization", "Bearer " + hiringManagerJwt))
                .andExpect(status().isNoContent());
    }

    @Test
    void getCurrentOrganization_authenticatedUser_returnsOrganization() throws Exception {
        mockMvc.perform(get("/api/organization/me")
                        .header("Authorization", "Bearer " + hiringManagerJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(organization.getName()));
    }
}
