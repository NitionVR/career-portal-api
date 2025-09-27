package com.etalente.backend.service.impl;

import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.AuthenticationService;
import com.etalente.backend.service.EmailService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;

    public AuthenticationServiceImpl(UserRepository userRepository, EmailService emailService, JwtService jwtService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
    }

    @Override
    public void initiateMagicLinkLogin(String email) {
        // 1. Find or create the user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    // Set a default role for new users
                    // newUser.setRole(User.Role.CANDIDATE);
                    return userRepository.save(newUser);
                });

        // 2. Generate a short-lived magic link token
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), "", new ArrayList<>());
        String magicToken = jwtService.generateToken(userDetails);

        // 3. Construct the magic link URL
        String magicLink = "http://localhost:4200/auth/callback?token=" + magicToken; // This should be configurable

        // 4. Send the email
        emailService.sendMagicLink(email, magicLink);
    }
}
