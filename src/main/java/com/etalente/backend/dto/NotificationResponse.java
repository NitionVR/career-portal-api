package com.etalente.backend.dto;

import com.etalente.backend.model.Notification;
import com.etalente.backend.model.NotificationStatus;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationResponse {

    private Long id;
    private String type;
    private String title;
    private String content;
    private String recipientId;
    private NotificationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Map<String, Object> metadata;

    public NotificationResponse() {
    }

    // All-args constructor for mapping
    public NotificationResponse(Long id, String type, String title, String content, String recipientId, NotificationStatus status, LocalDateTime createdAt, LocalDateTime readAt, Map<String, Object> metadata) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.recipientId = recipientId;
        this.status = status;
        this.createdAt = createdAt;
        this.readAt = readAt;
        this.metadata = metadata;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static NotificationResponse fromEntity(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRecipientId(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getMetadata()
        );
    }
}
