package com.etalente.backend.service;

import com.etalente.backend.model.NotificationPreference;
import com.etalente.backend.repository.NotificationPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class NotificationPreferenceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public NotificationPreference updatePreference(String userId, String notificationType,
                                                   boolean inApp, boolean email,
                                                   boolean push, boolean sms) {
        Optional<NotificationPreference> existingPreference = preferenceRepository
            .findByUserIdAndNotificationType(userId, notificationType);

        NotificationPreference preference = existingPreference.orElseGet(() -> {
            NotificationPreference newPreference = new NotificationPreference();
            newPreference.setUserId(userId);
            newPreference.setNotificationType(notificationType);
            return newPreference;
        });

        preference.setInAppEnabled(inApp);
        preference.setEmailEnabled(email);
        preference.setPushEnabled(push);
        preference.setSmsEnabled(sms);

        return preferenceRepository.save(preference);
    }

    @Transactional(readOnly = true)
    public List<NotificationPreference> getUserPreferences(String userId) {
        return preferenceRepository.findByUserId(userId);
    }
}
