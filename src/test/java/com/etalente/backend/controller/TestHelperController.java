package com.etalente.backend.controller;

import com.etalente.backend.model.RecruiterInvitation;
import com.etalente.backend.model.RegistrationToken;
import com.etalente.backend.repository.RecruiterInvitationRepository;
import com.etalente.backend.repository.RegistrationTokenRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Profile("test")
public class TestHelperController {

    private final RegistrationTokenRepository registrationTokenRepository;
    private final RecruiterInvitationRepository recruiterInvitationRepository;

    public TestHelperController(RegistrationTokenRepository registrationTokenRepository, RecruiterInvitationRepository recruiterInvitationRepository) {
        this.registrationTokenRepository = registrationTokenRepository;
        this.recruiterInvitationRepository = recruiterInvitationRepository;
    }

    @GetMapping("/token")
    public Map<String, String> getToken(@RequestParam String email) {
        RegistrationToken token = registrationTokenRepository.findAll().stream()
                .filter(t -> t.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No token found for email: " + email));

        return Map.of("token", token.getToken());
    }

    @GetMapping("/invitation-token")
    public Map<String, String> getInvitationToken(@RequestParam String email) {
        RecruiterInvitation invitation = recruiterInvitationRepository.findByEmail(email).stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No invitation found for email: " + email));

        return Map.of("token", invitation.getToken());
    }
}