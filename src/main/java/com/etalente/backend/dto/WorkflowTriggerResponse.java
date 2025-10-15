package com.etalente.backend.dto;

public class WorkflowTriggerResponse {

    private String transactionId;
    private boolean acknowledged;
    private String status;

    public WorkflowTriggerResponse() {
    }

    // Getters
    public String getTransactionId() {
        return transactionId;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public String getStatus() {
        return status;
    }

    // Setters
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
