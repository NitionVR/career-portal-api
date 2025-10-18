package com.etalente.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    private long maxFileSize;
    private List<String> allowedContentTypes;
    private int presignedUrlExpirationMinutes;

    // Getters and Setters
    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }

    public int getPresignedUrlExpirationMinutes() {
        return presignedUrlExpirationMinutes;
    }

    public void setPresignedUrlExpirationMinutes(int presignedUrlExpirationMinutes) {
        this.presignedUrlExpirationMinutes = presignedUrlExpirationMinutes;
    }
}
