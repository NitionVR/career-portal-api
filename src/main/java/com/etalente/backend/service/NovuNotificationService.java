package com.etalente.backend.service;

import com.etalente.backend.model.User;

public interface NovuNotificationService {
    void sendWelcomeNotification(User user);
    // Add other notification methods as needed (e.g., sendJobApplicationReceivedNotification)
}