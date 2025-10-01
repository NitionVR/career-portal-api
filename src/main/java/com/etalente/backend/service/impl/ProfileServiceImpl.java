package com.etalente.backend.service.impl;

import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.service.ProfileService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    public ProfileServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void updateProfile(String userEmail, Map<String, String> profileData) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        user.setFirstName(profileData.get("firstName"));
        user.setLastName(profileData.get("lastName"));
        user.setContactNumber(profileData.get("contactNumber"));
        user.setSummary(profileData.get("summary"));
        user.setProfileComplete(true);

        userRepository.save(user);
    }
}