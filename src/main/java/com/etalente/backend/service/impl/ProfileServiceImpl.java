package com.etalente.backend.service.impl;

import com.etalente.backend.dto.CompleteProfileRequest;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public ProfileServiceImpl(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void updateProfile(UUID userId, Map<String, String> profileData) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        profileData.forEach((key, value) -> {
            switch (key) {
                case "firstName":
                    user.setFirstName(value);
                    break;
                case "lastName":
                    user.setLastName(value);
                    break;
                case "contactNumber":
                    user.setContactNumber(value);
                    break;
                case "summary":
                    user.setSummary(value);
                    break;
                // Add other fields as necessary
            }
        });

        user.setProfileComplete(true);
        userRepository.save(user);
    }

    @Override
    public VerifyTokenResponse completeProfile(UUID userId, CompleteProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update user details
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (user.getRole() == Role.HIRING_MANAGER && request.getCompanyName() != null) {
            user.setCompanyName(request.getCompanyName());
        }
        user.setProfileComplete(true);
        user.setNewUser(false); // Mark profile as completed

        User updatedUser = userRepository.save(user);

        // Generate a new token with isNewUser=false
        String newToken = jwtService.generateToken(
                updatedUser.getId().toString(),
                updatedUser.getEmail(),
                updatedUser.getRole().name(),
                false, // isNewUser is now false
                updatedUser.getUsername()
        );

        VerifyTokenResponse.UserDto userDto = new VerifyTokenResponse.UserDto(
                updatedUser.getId().toString(),
                updatedUser.getEmail(),
                updatedUser.getRole().name(),
                updatedUser.getFirstName(),
                updatedUser.getLastName(),
                false // isNewUser is now false
        );

        long expiresIn = jwtService.getTimeUntilExpiration(newToken);

        return new VerifyTokenResponse(newToken, userDto, expiresIn);
    }
}