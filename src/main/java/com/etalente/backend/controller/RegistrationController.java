package com.etalente.backend.controller;

import com.etalente.backend.dto.CandidateRegistrationDto;
import com.etalente.backend.dto.HiringManagerRegistrationDto;
import com.etalente.backend.dto.RegistrationRequest;
import com.etalente.backend.model.User;
import com.etalente.backend.service.AuthenticationService;
import com.etalente.backend.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/register")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;

    public RegistrationController(RegistrationService registrationService, AuthenticationService authenticationService) {
        this.registrationService = registrationService;
        this.authenticationService = authenticationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> initiateRegistration(@Valid @RequestBody RegistrationRequest request) {
        registrationService.initiateRegistration(request);
        return ResponseEntity.ok(Map.of("message", "Registration link sent to your email"));
    }

    @PostMapping("/candidate")
    public ResponseEntity<Map<String, String>> completeCandidateRegistration(
            @RequestParam String token,
            @Valid @RequestBody CandidateRegistrationDto dto) {
        User user = registrationService.completeRegistration(token, dto);
        String jwt = authenticationService.generateJwtForUser(user);
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @PostMapping("/hiring-manager")
    public ResponseEntity<Map<String, String>> completeHiringManagerRegistration(
            @RequestParam String token,
            @Valid @RequestBody HiringManagerRegistrationDto dto) {
        User user = registrationService.completeRegistration(token, dto);
        String jwt = authenticationService.generateJwtForUser(user);
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateRegistrationToken(@RequestParam String token) {
        Map<String, Object> tokenInfo = registrationService.validateToken(token);
        return ResponseEntity.ok(tokenInfo);
    }
}
