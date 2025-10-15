package com.etalente.backend.repository;

import com.etalente.backend.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserIdAndNotificationType(String userId, String notificationType);

    List<NotificationPreference> findByUserId(String userId);
}
