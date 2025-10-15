package com.etalente.backend.dto;

public class RegistrationResponse {
    private String token;

    public RegistrationResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}