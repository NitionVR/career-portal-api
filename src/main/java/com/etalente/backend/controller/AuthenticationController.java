package com.etalente.backend.controller;

import com.etalente.backend.dto.*;
import com.etalente.backend.model.User;
import com.etalente.backend.service.AuthenticationService;
import com.etalente.backend.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;

    public AuthenticationController(AuthenticationService authenticationService,
                                  RegistrationService registrationService) {
        this.authenticationService = authenticationService;
        this.registrationService = registrationService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authenticationService.initiateMagicLinkLogin(email);
        return ResponseEntity.ok("Magic link sent. Please check your email.");
    }

    @GetMapping("/verify")
    public ResponseEntity<RegistrationResponse> verify(@RequestParam String token) {
        String jwt = authenticationService.verifyMagicLinkAndIssueJwt(token);
        return ResponseEntity.ok(new RegistrationResponse(jwt));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> initiateRegistration(@Valid @RequestBody RegistrationRequest request) {
        registrationService.initiateRegistration(request);
        return ResponseEntity.ok(Map.of("message", "Registration link sent to your email"));
    }

    @PostMapping("/register/candidate")
    public ResponseEntity<Map<String, String>> completeCandiateRegistration(
            @RequestParam String token,
            @Valid @RequestBody CandidateRegistrationDto dto) {
        User user = registrationService.completeRegistration(token, dto);
        String jwt = authenticationService.generateJwtForUser(user);
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @PostMapping("/register/hiring-manager")
    public ResponseEntity<Map<String, String>> completeHiringManagerRegistration(
            @RequestParam String token,
            @Valid @RequestBody HiringManagerRegistrationDto dto) {
        User user = registrationService.completeRegistration(token, dto);
        String jwt = authenticationService.generateJwtForUser(user);
        return ResponseEntity.ok(Map.of("token", jwt));
    }

    @GetMapping("/validate-registration-token")
    public ResponseEntity<Map<String, Object>> validateRegistrationToken(@RequestParam String token) {
        Map<String, Object> tokenInfo = registrationService.validateToken(token);
        return ResponseEntity.ok(tokenInfo);
    }
}