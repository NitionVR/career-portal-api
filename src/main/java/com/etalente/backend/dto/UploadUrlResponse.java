package com.etalente.backend.dto;

public class UploadUrlResponse {

    private String uploadUrl;  // Pre-signed URL for uploading
    private String fileUrl;     // Final public URL of the file
    private String key;         // S3 key (for backend reference)

    public UploadUrlResponse() {}

    public UploadUrlResponse(String uploadUrl, String fileUrl, String key) {
        this.uploadUrl = uploadUrl;
        this.fileUrl = fileUrl;
        this.key = key;
    }

    // Getters and setters
    public String getUploadUrl() {
        return uploadUrl;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
