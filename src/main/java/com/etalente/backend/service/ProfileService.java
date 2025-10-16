package com.etalente.backend.service;

import java.util.Map;
import java.util.UUID;

public interface ProfileService {
    void updateProfile(UUID userId, Map<String, String> profileData);
}
