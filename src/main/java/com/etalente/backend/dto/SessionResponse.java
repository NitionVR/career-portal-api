package com.etalente.backend.dto;

public class SessionResponse {

    private boolean authenticated;
    private String userId;
    private String email;
    private String role;
    private Boolean isNewUser;
    private Long expiresIn;

    // Static factory methods
    public static SessionResponse authenticated(String userId, String email,
                                               String role, Boolean isNewUser,
                                               long expiresIn) {
        SessionResponse response = new SessionResponse();
        response.authenticated = true;
        response.userId = userId;
        response.email = email;
        response.role = role;
        response.isNewUser = isNewUser;
        response.expiresIn = expiresIn;
        return response;
    }

    public static SessionResponse unauthenticated() {
        SessionResponse response = new SessionResponse();
        response.authenticated = false;
        return response;
    }

    // Getters and setters
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsNewUser() { return isNewUser; }
    public void setIsNewUser(Boolean isNewUser) { this.isNewUser = isNewUser; }

    public Long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }
}
