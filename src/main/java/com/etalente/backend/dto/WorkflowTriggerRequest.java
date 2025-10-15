package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class WorkflowTriggerRequest {

    @NotBlank(message = "Subscriber ID is required")
    private String subscriberId;

    private String email;
    private String firstName;
    private String lastName;

    @NotNull(message = "Payload is required")
    private Map<String, Object> payload;

    public WorkflowTriggerRequest() {
    }

    // Getters
    public String getSubscriberId() {
        return subscriberId;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    // Setters
    public void setSubscriberId(String subscriberId) {
        this.subscriberId = subscriberId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
