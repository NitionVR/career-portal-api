package com.etalente.backend.service;

import java.util.Map;

public interface ProfileService {
    void updateProfile(String userEmail, Map<String, String> profileData);
}
