package com.etalente.backend.repository;

import com.etalente.backend.model.Notification;
import com.etalente.backend.model.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    List<Notification> findByRecipientIdAndStatus(String recipientId, NotificationStatus status);

    long countByRecipientIdAndStatus(String recipientId, NotificationStatus status);

    @Query("SELECT n FROM Notification n WHERE n.recipientId = :recipientId " +
           "AND n.status = 'PENDING' AND n.createdAt < :before")
    List<Notification> findStaleNotifications(String recipientId, LocalDateTime before);
}
