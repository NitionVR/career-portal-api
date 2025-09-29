package com.etalente.backend.service.impl;

import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.AuthenticationService;
import com.etalente.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;

    @Value("${application.magic-link-url}")
    private String magicLinkUrl;

    public AuthenticationServiceImpl(UserRepository userRepository, EmailService emailService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    @Override
    public void initiateMagicLinkLogin(String email) {
        // 1. Find the user, determining if they are new or existing
        Optional<User> userOptional = userRepository.findByEmail(email);
        final boolean isNewUser = userOptional.isEmpty();

        User user = userOptional.orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setRole(com.etalente.backend.model.Role.CANDIDATE);
            return userRepository.save(newUser);
        });

        // 2. Generate a short-lived magic link token with the is_new_user claim
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), "", new ArrayList<>());
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("is_new_user", isNewUser);
        String magicToken = jwtService.generateToken(extraClaims, userDetails);

        // 3. Construct the magic link URL
        String magicLink = magicLinkUrl + "?token=" + magicToken;

        // 4. Send the email
        emailService.sendMagicLink(email, magicLink);
    }

    @Override
    public String verifyMagicLinkAndIssueJwt(String token) {
        final String email = jwtService.extractUsername(token);
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(email, "", new ArrayList<>());

        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new RuntimeException("Invalid or expired magic link token.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found for the given token."));

        // Token is valid and user exists, issue a new session JWT
        return jwtService.generateToken(userDetails);
    }
}
