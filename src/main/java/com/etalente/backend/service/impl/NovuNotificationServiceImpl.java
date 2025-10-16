package com.etalente.backend.service.impl;

import co.novu.api.events.requests.TriggerEventRequest;
import co.novu.api.events.pojos.SubscriberRequest;
import co.novu.common.base.Novu;
import co.novu.common.rest.NovuNetworkException;
import com.etalente.backend.model.User;
import com.etalente.backend.service.NovuNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NovuNotificationServiceImpl implements NovuNotificationService {

    private final Novu novu;

    @Override
    public void sendWelcomeNotification(User user) {
        if (user.getEmail() == null || user.getFirstName() == null) {
            log.warn("Cannot send welcome notification: User email or first name is missing for user ID {}", user.getId());
            return;
        }

        try {
            TriggerEventRequest triggerRequest = getTriggerEventRequest(user);

            novu.triggerEvent(triggerRequest);
            log.info("Successfully triggered welcome email for user {}", user.getEmail());
        } catch (IOException | NovuNetworkException e) {
            log.error("Failed to trigger welcome email for user {}: {}", user.getEmail(), e.getMessage(), e);
            // Depending on criticality, you might rethrow a custom application exception
            // throw new NotificationException("Failed to send welcome email", e);
        }
    }

    @NotNull
    private TriggerEventRequest getTriggerEventRequest(User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", user.getFirstName());
        // Add other variables needed by the template

        SubscriberRequest subscriberRequest = new SubscriberRequest();
        subscriberRequest.setSubscriberId(user.getId().toString());
        subscriberRequest.setEmail(user.getEmail());
        subscriberRequest.setFirstName(user.getFirstName());
        subscriberRequest.setLastName(user.getLastName());

        TriggerEventRequest triggerRequest = new TriggerEventRequest();
        triggerRequest.setName("welcome-email-workflow");
        triggerRequest.setTo(subscriberRequest);
        triggerRequest.setPayload(payload);
        return triggerRequest;
    }
}
