package com.etalente.backend.service;

import com.etalente.backend.dto.NotificationRequest;
import com.etalente.backend.dto.NotificationResponse;
import com.etalente.backend.model.Notification;
import com.etalente.backend.model.NotificationPreference;
import com.etalente.backend.model.NotificationStatus;
import com.etalente.backend.repository.NotificationPreferenceRepository;
import com.etalente.backend.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               NotificationPreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
    }

    public NotificationResponse createNotification(NotificationRequest request) {
        logger.info("Creating notification of type {} for recipient {}",
                request.getType(), request.getRecipientId());

        // Check if notification is allowed
        if (!isNotificationAllowed(request.getRecipientId(), request.getType())) {
            logger.info("Notification blocked by user preferences");
            return null;
        }

        Notification notification = new Notification();
        notification.setType(request.getType());
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setRecipientId(request.getRecipientId());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setMetadata(request.getMetadata());

        notification = notificationRepository.save(notification);

        logger.info("Notification created with ID: {}", notification.getId());

        return NotificationResponse.fromEntity(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(String userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
            .map(NotificationResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdAndStatus(userId, NotificationStatus.PENDING);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId)
            .ifPresent(notification -> {
                notification.markAsRead();
                notificationRepository.save(notification);
                logger.info("Notification {} marked as read", notificationId);
            });
    }

    public void markAllAsRead(String userId) {
        notificationRepository.findByRecipientIdAndStatus(userId, NotificationStatus.PENDING)
            .forEach(notification -> {
                notification.markAsRead();
                notificationRepository.save(notification);
            });
        logger.info("All notifications marked as read for user {}", userId);
    }

    private boolean isNotificationAllowed(String userId, String notificationType) {
        Optional<NotificationPreference> preference =
            preferenceRepository.findByUserIdAndNotificationType(userId, notificationType);

        // Default to allowed if no preference is set
        return preference.map(NotificationPreference::isInAppEnabled).orElse(true);
    }
}
