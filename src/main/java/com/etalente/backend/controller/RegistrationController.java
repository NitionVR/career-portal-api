package com.etalente.backend.controller;

import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/register")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ResponseEntity<Void> initiateRegistration(@Valid @RequestBody RegistrationRequest request) {
        registrationService.initiateRegistration(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/candidate")
    public ResponseEntity<VerifyTokenResponse> completeCandidateRegistration(@RequestParam String token,
                                                                             @Valid @RequestBody CandidateRegistrationDto dto) {
        VerifyTokenResponse response = registrationService.completeRegistration(token, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/hiring-manager")
    public ResponseEntity<VerifyTokenResponse> completeHiringManagerRegistration(@RequestParam String token,
                                                                                     @Valid @RequestBody HiringManagerRegistrationDto dto) {
        VerifyTokenResponse response = registrationService.completeRegistration(token, dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateRegistrationToken(@RequestParam String token) {
        Map<String, Object> validationResult = registrationService.validateToken(token);
        return ResponseEntity.ok(validationResult);
    }
}
