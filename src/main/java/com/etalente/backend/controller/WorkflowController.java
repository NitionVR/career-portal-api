package com.etalente.backend.controller;

import com.etalente.backend.dto.WorkflowTriggerRequest;
import com.etalente.backend.dto.WorkflowTriggerResponse;
import com.etalente.backend.integration.novu.NovuWorkflowService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowController.class);

    private final NovuWorkflowService novuWorkflowService;

    public WorkflowController(NovuWorkflowService novuWorkflowService) {
        this.novuWorkflowService = novuWorkflowService;
    }

    @PostMapping("/{workflowId}/trigger")
    public ResponseEntity<WorkflowTriggerResponse> triggerWorkflow(
            @PathVariable String workflowId,
            @Valid @RequestBody WorkflowTriggerRequest request) {

        logger.info("Received workflow trigger request for: {}", workflowId);

        try {
            // Trigger asynchronously and return 202 Accepted
            novuWorkflowService.triggerWorkflow(workflowId, request);
            return ResponseEntity.accepted().build();

        } catch (Exception e) {
            logger.error("Error triggering workflow", e);

            WorkflowTriggerResponse errorResponse = new WorkflowTriggerResponse();
            errorResponse.setAcknowledged(false);
            errorResponse.setStatus("FAILED: " + e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
