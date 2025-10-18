package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.TestHelper;
import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.CandidateRegistrationRequest;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationRequest;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.model.RegistrationToken;
import com.etalente.backend.model.Role;
import com.etalente.backend.repository.RegistrationTokenRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistrationControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RegistrationTokenRepository registrationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestHelper testHelper;

    @Test
    void initiateRegistration_shouldReturnOk_forNewEmail() throws Exception {
        RegistrationRequest request = new RegistrationRequest("newuser." + UUID.randomUUID() + "@example.com", Role.CANDIDATE);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void completeCandidateRegistration_shouldCreateUserAndReturnTokenResponse() throws Exception {
        String token = UUID.randomUUID().toString();
        String email = "candidate." + UUID.randomUUID() + "@example.com";
        RegistrationToken registrationToken = new RegistrationToken(token, email, Role.CANDIDATE, LocalDateTime.now().plusMinutes(15));
        registrationTokenRepository.save(registrationToken);

        CandidateRegistrationDto dto = new CandidateRegistrationDto("candidateuser", "Candidate", "User", null, null, null, "+1234567890", null);
        CandidateRegistrationRequest request = new CandidateRegistrationRequest(dto);

        mockMvc.perform(post("/api/register/candidate").param("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.isNewUser").value(false));

        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Test
    void completeHiringManagerRegistration_shouldCreateUserAndOrgAndReturnTokenResponse() throws Exception {
        String token = UUID.randomUUID().toString();
        String email = "hm." + UUID.randomUUID() + "@example.com";
        RegistrationToken registrationToken = new RegistrationToken(token, email, Role.HIRING_MANAGER, LocalDateTime.now().plusMinutes(15));
        registrationTokenRepository.save(registrationToken);

        HiringManagerRegistrationDto dto = new HiringManagerRegistrationDto("hmuser", "Test Company", "Tech", "HM Contact", "+1234567890");
        HiringManagerRegistrationRequest request = new HiringManagerRegistrationRequest(dto);

        mockMvc.perform(post("/api/register/hiring-manager").param("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.isNewUser").value(false));

        assertThat(userRepository.findByEmail(email)).isPresent();
        assertThat(userRepository.findByEmail(email).get().getOrganization()).isNotNull();
    }
}
