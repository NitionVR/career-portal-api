package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public class UploadUrlRequest {

    @NotBlank(message = "Content type is required")
    private String contentType;

    @Positive(message = "Content length must be positive")
    private long contentLength;

    public UploadUrlRequest() {}

    public UploadUrlRequest(String contentType, long contentLength) {
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    // Getters and setters
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}
