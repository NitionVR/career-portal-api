package com.etalente.backend.service.impl;

import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.OneTimeToken;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OneTimeTokenRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.AuthenticationService;
import com.etalente.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final OneTimeTokenRepository oneTimeTokenRepository;

    @Value("${magic-link.url}")
    private String magicLinkUrl;

    @Value("${ott.expiration-minutes}")
    private long ottExpirationMinutes;

    public AuthenticationServiceImpl(UserRepository userRepository, EmailService emailService, JwtService jwtService, OneTimeTokenRepository oneTimeTokenRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtService = jwtService;
        this.oneTimeTokenRepository = oneTimeTokenRepository;
    }

    @Override
    @Transactional
    public void sendMagicLink(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email + " not found."));

        String ott = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(ottExpirationMinutes);

        OneTimeToken oneTimeToken = new OneTimeToken(ott, user, expiryDate);
        oneTimeTokenRepository.save(oneTimeToken);

        String magicLink = magicLinkUrl + "?token=" + ott; // Changed from ott= to token= to match frontend expectations

        emailService.sendMagicLink(email, magicLink);
    }

    @Override
    @Transactional
    public VerifyTokenResponse exchangeOneTimeTokenForJwt(String ott) {
        OneTimeToken oneTimeToken = oneTimeTokenRepository.findByToken(ott)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired one-time token."));

        if (oneTimeToken.isUsed() || oneTimeToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Invalid or expired one-time token.");
        }

        oneTimeToken.setUsed(true);
        oneTimeTokenRepository.save(oneTimeToken);

        User user = oneTimeToken.getUser();
        if (user == null) {
            throw new ResourceNotFoundException("User not found for the provided token.");
        }

        boolean isNewUser = user.isNewUser();

        // If the user is new, update the flag to false after this first login
        if (isNewUser) {
            user.setNewUser(false);
            userRepository.save(user);
        }

        String jwt = jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                isNewUser,
                user.getUsername()
        );

        VerifyTokenResponse.UserDto userDto = new VerifyTokenResponse.UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName(),
                isNewUser,
                user.getProfileImageUrl() // Pass the actual profileImageUrl
        );

        long expiresIn = jwtService.getTimeUntilExpiration(jwt);

        return new VerifyTokenResponse(jwt, userDto, expiresIn);
    }

    @Override
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    @Override
    public String generateJwtForUser(User user) {
        return jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                false, // Assume not a new user when generating token directly
                user.getUsername()
        );
    }
}
