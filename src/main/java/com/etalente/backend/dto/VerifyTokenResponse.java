package com.etalente.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VerifyTokenResponse {

    private String token;
    private UserDto user;
    private long expiresIn;

    public static class UserDto {
        private String id;
        private String email;
        private String role;
        private String firstName;
        private String lastName;
        private boolean isNewUser;
        private String profileImageUrl;

        public UserDto(String id, String email, String role, String firstName,
                      String lastName, boolean isNewUser, String profileImageUrl) {
            this.id = id;
            this.email = email;
            this.role = role;
            this.firstName = firstName;
            this.lastName = lastName;
            this.isNewUser = isNewUser;
            this.profileImageUrl = profileImageUrl;
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        @JsonProperty("isNewUser")
        public boolean isNewUser() { return isNewUser; }
        public void setNewUser(boolean newUser) { isNewUser = newUser; }

        public String getProfileImageUrl() { return profileImageUrl; }
        public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    }

    public VerifyTokenResponse(String token, UserDto user, long expiresIn) {
        this.token = token;
        this.user = user;
        this.expiresIn = expiresIn;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UserDto getUser() { return user; }
    public void setUser(UserDto user) { this.user = user; }

    public long getExpiresIn() { return expiresIn; }
    public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
}
