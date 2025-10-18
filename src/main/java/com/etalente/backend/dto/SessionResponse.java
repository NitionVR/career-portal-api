package com.etalente.backend.dto;

public class SessionResponse {

    private boolean authenticated;
    private VerifyTokenResponse.UserDto user;
    private Long expiresIn;

    // Private constructor to force use of static factory methods
    private SessionResponse() {}

    public static SessionResponse authenticated(VerifyTokenResponse.UserDto user, long expiresIn) {
        SessionResponse response = new SessionResponse();
        response.authenticated = true;
        response.user = user;
        response.expiresIn = expiresIn;
        return response;
    }

    public static SessionResponse unauthenticated() {
        SessionResponse response = new SessionResponse();
        response.authenticated = false;
        return response;
    }

    // Getters
    public boolean isAuthenticated() {
        return authenticated;
    }

    public VerifyTokenResponse.UserDto getUser() {
        return user;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }
}
