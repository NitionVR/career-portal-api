package com.etalente.backend.service.impl;

import com.etalente.backend.model.OneTimeToken;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OneTimeTokenRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.AuthenticationService;
import com.etalente.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final OneTimeTokenRepository oneTimeTokenRepository; // New

    @Value("${magic-link-url}")
    private String magicLinkUrl;

    @Value("${ott.expiration-minutes}") // New
    private long ottExpirationMinutes;

    public AuthenticationServiceImpl(UserRepository userRepository, EmailService emailService, JwtService jwtService, OneTimeTokenRepository oneTimeTokenRepository) { // Updated constructor
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.oneTimeTokenRepository = oneTimeTokenRepository; // New
    }

    @Override
    public void initiateMagicLinkLogin(String email) {
        // 1. Find the user, determining if they are new or existing
        Optional<User> userOptional = userRepository.findByEmail(email);

        User user = userOptional.orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setRole(com.etalente.backend.model.Role.CANDIDATE);
            return userRepository.save(newUser);
        });

        // 2. Generate a cryptographically secure random string for the OTT
        String ott = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(ottExpirationMinutes);

        // 3. Store the OTT in the database
        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, expiryDate);
        oneTimeTokenRepository.save(oneTimeToken);

        // 4. Construct the magic link URL with the OTT
        String magicLink = magicLinkUrl + "?ott=" + ott;

        // 5. Send the email
        emailService.sendMagicLink(email, magicLink);
    }

    @Override
    public String exchangeOneTimeTokenForJwt(String ott) {
        OneTimeToken oneTimeToken = oneTimeTokenRepository.findByToken(ott)
                .orElseThrow(() -> new RuntimeException("Invalid or expired one-time token."));

        if (oneTimeToken.isUsed() || oneTimeToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invalid or expired one-time token.");
        }

        oneTimeToken.setUsed(true);
        oneTimeTokenRepository.save(oneTimeToken);

        // Fetch the user explicitly within the transaction to ensure it's managed
        User user = userRepository.findById(oneTimeToken.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found for one-time token."));

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), "", new ArrayList<>());

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());

        return jwtService.generateToken(claims, userDetails);
    }

    @Override
    public String generateJwtForUser(User user) {
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                "",
                new ArrayList<>()
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        if (user.getRole() != null) {
            claims.put("role", user.getRole().name());
        }
        if (user.getUsername() != null) {
            claims.put("username", user.getUsername());
        }

        return jwtService.generateToken(claims, userDetails);
    }
}
