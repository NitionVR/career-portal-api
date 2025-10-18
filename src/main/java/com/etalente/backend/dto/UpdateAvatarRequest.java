package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class UpdateAvatarRequest {

    @NotBlank(message = "Profile image URL is required")
    @URL(protocol = "https", message = "Profile image URL must be a valid HTTPS URL")
    private String profileImageUrl;

    public UpdateAvatarRequest() {}

    public UpdateAvatarRequest(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
