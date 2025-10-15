package com.etalente.backend.integration.novu;

import co.novu.api.common.SubscriberRequest;
import co.novu.api.subscribers.responses.CreateSubscriberResponse;
import co.novu.common.base.Novu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NovuSubscriberService {

    private static final Logger logger = LoggerFactory.getLogger(NovuSubscriberService.class);

    private final Novu novuClient;

    public NovuSubscriberService(Novu novuClient) {
        this.novuClient = novuClient;
    }

    public void createOrUpdateSubscriber(String subscriberId, String email,
                                         String firstName, String lastName) {
        logger.info("Creating/updating Novu subscriber: {}", subscriberId);

        try {
            SubscriberRequest request = new SubscriberRequest();
            request.setSubscriberId(subscriberId);
            request.setEmail(email);
            request.setFirstName(firstName);
            request.setLastName(lastName);

            CreateSubscriberResponse response = novuClient.createSubscriber(request);
            logger.info("Subscriber created/updated successfully: {}", subscriberId);

        } catch (Exception e) {
            logger.error("Failed to create/update subscriber: {}", subscriberId, e);
        }
    }

    public void updateSubscriberPreferences(String subscriberId,
                                           Map<String, Boolean> preferences) {
        logger.info("Updating subscriber preferences: {}", subscriberId);

        try {
            // Update preferences via Novu API
            // Implementation depends on Novu SDK capabilities

        } catch (Exception e) {
            logger.error("Failed to update subscriber preferences: {}", subscriberId, e);
        }
    }
}
