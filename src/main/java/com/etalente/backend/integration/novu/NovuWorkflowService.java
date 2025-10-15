package com.etalente.backend.integration.novu;

import co.novu.api.common.SubscriberRequest;
import co.novu.api.events.requests.TriggerEventRequest;
import co.novu.api.events.responses.TriggerEventResponse;
import co.novu.common.base.Novu;
import com.etalente.backend.dto.WorkflowTriggerRequest;
import com.etalente.backend.dto.WorkflowTriggerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class NovuWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(NovuWorkflowService.class);

    private final Novu novuClient;

    public NovuWorkflowService(Novu novuClient) {
        this.novuClient = novuClient;
    }

    @Async
    public CompletableFuture<WorkflowTriggerResponse> triggerWorkflow(String workflowId, WorkflowTriggerRequest request) {
        logger.info("Triggering Novu workflow: {} for subscriber: {}", workflowId, request.getSubscriberId());

        try {
            SubscriberRequest subscriberRequest = new SubscriberRequest();
            subscriberRequest.setSubscriberId(request.getSubscriberId());
            subscriberRequest.setEmail(request.getEmail());
            subscriberRequest.setFirstName(request.getFirstName());
            subscriberRequest.setLastName(request.getLastName());

            TriggerEventRequest triggerEvent = new TriggerEventRequest();
            triggerEvent.setName(workflowId);
            triggerEvent.setTo(subscriberRequest);
            triggerEvent.setPayload(request.getPayload());

            TriggerEventResponse response = novuClient.triggerEvent(triggerEvent);

            logger.info("Workflow triggered successfully. Transaction ID: {}",
                    response.getData().getTransactionId());

            WorkflowTriggerResponse workflowTriggerResponse = new WorkflowTriggerResponse();
            workflowTriggerResponse.setTransactionId(response.getData().getTransactionId());
            workflowTriggerResponse.setAcknowledged(response.getData().isAcknowledged());
            workflowTriggerResponse.setStatus("SUCCESS");
            return CompletableFuture.completedFuture(workflowTriggerResponse);

        } catch (Exception e) {
            logger.error("Failed to trigger Novu workflow: {}", workflowId, e);

            WorkflowTriggerResponse workflowTriggerResponse = new WorkflowTriggerResponse();
            workflowTriggerResponse.setAcknowledged(false);
            workflowTriggerResponse.setStatus("FAILED");

            return CompletableFuture.completedFuture(workflowTriggerResponse);
        }
    }
}
