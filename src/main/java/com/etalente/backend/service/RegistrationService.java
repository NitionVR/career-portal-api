package com.etalente.backend.service;

import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.model.User;

import java.util.Map;

public interface RegistrationService {
    void initiateRegistration(RegistrationRequest request);
    User completeRegistration(String token, CandidateRegistrationDto dto);
    User completeRegistration(String token, HiringManagerRegistrationDto dto);
    Map<String, Object> validateToken(String token);
}
