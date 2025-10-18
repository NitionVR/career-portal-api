package com.etalente.backend.service;

import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.model.User;

import java.util.Map;

public interface RegistrationService {
    void initiateRegistration(RegistrationRequest request);
    VerifyTokenResponse completeRegistration(String token, CandidateRegistrationDto dto);
    VerifyTokenResponse completeRegistration(String token, HiringManagerRegistrationDto dto);
    Map<String, Object> validateToken(String token);
}
