package com.etalente.backend.dto;

import com.etalente.backend.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UserDto {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String profileImageUrl;
    private boolean isNewUser;

    // Constructor
    public UserDto(String id, String email, String firstName, String lastName,
                  String role, String profileImageUrl, boolean isNewUser) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.profileImageUrl = profileImageUrl;
        this.isNewUser = isNewUser;
    }

    // Factory method
    public static UserDto fromEntity(User user) {
        return new UserDto(
            user.getId().toString(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().name(),
            user.getProfileImageUrl(),
            user.isNewUser()
        );
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    @JsonProperty("isNewUser")
    public boolean isNewUser() { return isNewUser; }
    public void setNewUser(boolean newUser) { isNewUser = newUser; }
}
