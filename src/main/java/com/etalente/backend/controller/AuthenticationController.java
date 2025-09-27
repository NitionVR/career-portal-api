package com.etalente.backend.controller;

import com.etalente.backend.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        authenticationService.initiateMagicLinkLogin(email);
        return ResponseEntity.ok("Magic link sent. Please check your email.");
    }

    @GetMapping("/verify")
    public ResponseEntity<String> verify(@RequestParam String token) {
        String jwt = authenticationService.verifyMagicLinkAndIssueJwt(token);
        return ResponseEntity.ok(jwt);
    }
}
