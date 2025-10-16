package com.etalente.backend.integration.novu;

import co.novu.api.blueprints.pojos.Content;
import co.novu.api.blueprints.pojos.Variables;
import co.novu.api.common.PreferenceSettings;
import co.novu.api.common.Step;
import co.novu.api.workflowgroups.responses.WorkflowGroupResponseData;
import co.novu.api.workflows.requests.WorkflowRequest;
import co.novu.api.workflows.responses.SingleWorkflowResponse;
import co.novu.api.workflowgroups.request.WorkflowGroupRequest;
import co.novu.api.workflowgroups.responses.GetWorkflowGroupsResponse;
import co.novu.api.workflowgroups.responses.WorkflowGroupResponse;
import co.novu.common.base.Novu;
import co.novu.common.rest.NovuNetworkException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NovuWorkflowCreator {

    private final Novu novu;

    public SingleWorkflowResponse createWelcomeEmailWorkflow() {
        try {
            // 1. Define the email content template (as a Map for direct JSON serialization)
            Map<String, Object> emailTemplateMap = new HashMap<>();
            emailTemplateMap.put("type", "email"); // This is the crucial 'type' field
            emailTemplateMap.put("name", "Welcome Email Template");
            emailTemplateMap.put("subject", "Welcome to Etalente!");
            emailTemplateMap.put("content", "<h1>Welcome, {{firstName}}!</h1><p>Thank you for joining Etalente.</p>");
            emailTemplateMap.put("contentType", "html");

            // Variables for the template
            Map<String, Object> firstNameVariableMap = new HashMap<>();
            firstNameVariableMap.put("name", "firstName");
            firstNameVariableMap.put("type", "string");
            firstNameVariableMap.put("required", true);
            emailTemplateMap.put("variables", Collections.singletonList(firstNameVariableMap));

            // 2. Define the step for the workflow
            Step welcomeEmailStep = new Step();
            welcomeEmailStep.setName("Send Welcome Email");
            welcomeEmailStep.setActive(true);

            // Set the template as the Map directly. Gson will serialize it correctly.
            welcomeEmailStep.setTemplate(emailTemplateMap);

            // 3. Define preference settings for the workflow
            PreferenceSettings preferenceSettings = new PreferenceSettings();
            preferenceSettings.setEmail(true);
            preferenceSettings.setInApp(false);
            preferenceSettings.setSms(false);
            preferenceSettings.setPush(false);
            preferenceSettings.setChat(false);

            // 4. Create the WorkflowRequest
            WorkflowRequest workflowRequest = new WorkflowRequest();
            workflowRequest.setName("welcome-email-workflow"); // Unique identifier for the workflow
            workflowRequest.setDescription("Sends a welcome email to new users upon registration.");
            workflowRequest.setActive(true);
            workflowRequest.setDraft(false);
            workflowRequest.setTags(Arrays.asList("onboarding", "welcome"));
            workflowRequest.setPreferenceSettings(preferenceSettings);
            workflowRequest.setSteps(Collections.singletonList(welcomeEmailStep));

            // 4.1. Get or Create Notification Group
            String notificationGroupId = getOrCreateNotificationGroup();
            workflowRequest.setNotificationGroupId(notificationGroupId);

            // 5. Call Novu SDK to create the workflow
            SingleWorkflowResponse response = novu.createWorkflow(workflowRequest);
            log.info("Successfully created Novu workflow: {}", response.getData().getName());
            return response;

        } catch (IOException | NovuNetworkException e) {
            log.error("Failed to create Novu workflow: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Novu workflow", e);
        }
    }

    private String getOrCreateNotificationGroup() throws IOException, NovuNetworkException {
        final String DEFAULT_GROUP_NAME = "General";

        // Try to get existing notification groups
        GetWorkflowGroupsResponse groupsResponse = novu.getWorkflowGroups();
        if (groupsResponse != null && groupsResponse.getData() != null) {
            for (WorkflowGroupResponseData group : groupsResponse.getData()) {
                if (DEFAULT_GROUP_NAME.equals(group.getName())) {
                    log.info("Found existing Novu notification group: {}", DEFAULT_GROUP_NAME);
                    return group.getId();
                }
            }
        }

        // If not found, create a new one
        log.info("Novu notification group '{}' not found, creating a new one.", DEFAULT_GROUP_NAME);
        WorkflowGroupRequest createGroupRequest = new WorkflowGroupRequest();
        createGroupRequest.setName(DEFAULT_GROUP_NAME);
        WorkflowGroupResponse newGroupResponse = novu.createWorkflowGroup(createGroupRequest);
        log.info("Successfully created Novu notification group: {}", newGroupResponse.getData().getName());
        return newGroupResponse.getData().getId();
    }

}
