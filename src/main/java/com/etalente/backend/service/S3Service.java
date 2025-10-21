package com.etalente.backend.service;

import com.etalente.backend.config.UploadProperties;
import com.etalente.backend.dto.UploadUrlResponse;
import com.etalente.backend.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final UploadProperties uploadProperties;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.cloudfront-domain:}")
    private String cloudFrontDomain;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, UploadProperties uploadProperties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.uploadProperties = uploadProperties;
    }

    /**
     * Generate pre-signed URL for uploading a file
     *
     * @param folder - Folder in S3 (e.g., "avatars", "company-logos")
     * @param contentType - MIME type of the file
     * @param contentLength - File size in bytes
     * @return UploadUrlResponse with uploadUrl and fileUrl
     */
    public UploadUrlResponse generatePresignedUploadUrl(String folder, String contentType, long contentLength) {
        // Validate content type
        validateContentType(contentType);

        // Validate file size
        validateFileSize(contentLength);

        // Generate unique file name
        String fileName = generateFileName(contentType);
        String key = folder + "/" + fileName;

        try {
            // Create PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .metadata(java.util.Map.of(
                        "uploaded-by", "etalente-backend",
                        "upload-timestamp", String.valueOf(System.currentTimeMillis())
                    ))
                    .build();

            // Create presign request
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(uploadProperties.getPresignedUrlExpirationMinutes()))
                    .putObjectRequest(putObjectRequest)
                    .build();

            // Generate presigned URL
            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            String uploadUrl = presignedRequest.url().toString();

            // Generate public file URL
            String fileUrl = generateFileUrl(key);

            logger.info("Generated presigned upload URL for key: {}", key);

            return new UploadUrlResponse(uploadUrl, fileUrl, key);

        } catch (Exception e) {
            logger.error("Failed to generate presigned URL for folder: {}", folder, e);
            throw new RuntimeException("Failed to generate upload URL", e);
        }
    }

    /**
     * Delete a file from S3
     *
     * @param fileUrl - The public URL of the file
     */
    public void deleteFile(String fileUrl) {
        try {
            // Extract key from URL
            String key = extractKeyFromUrl(fileUrl);

            if (key == null) {
                logger.warn("Could not extract key from URL: {}", fileUrl);
                return;
            }

            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            logger.info("Deleted file from S3: {}", key);

        } catch (Exception e) {
            logger.error("Failed to delete file: {}", fileUrl, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    /**
     * Check if file exists in S3
     */
    public boolean fileExists(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            if (key == null) return false;

            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            logger.error("Error checking if file exists: {}", fileUrl, e);
            return false;
        }
    }

    /**
     * Validate content type
     */
    private void validateContentType(String contentType) {
        if (!uploadProperties.getAllowedContentTypes().contains(contentType)) {
            throw new BadRequestException(
                "Invalid content type: " + contentType +
                ". Allowed types: " + String.join(", ", uploadProperties.getAllowedContentTypes())
            );
        }
    }

    /**
     * Validate file size
     */
    private void validateFileSize(long contentLength) {
        if (contentLength > uploadProperties.getMaxFileSize()) {
            throw new BadRequestException(
                "File size exceeds maximum allowed size of " +
                (uploadProperties.getMaxFileSize() / 1024 / 1024) + "MB"
            );
        }

        if (contentLength <= 0) {
            throw new BadRequestException("Invalid file size");
        }
    }

    /**
     * Generate unique file name
     */
    private String generateFileName(String contentType) {
        String extension = getFileExtension(contentType);
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * Get file extension from content type
     */
    private String getFileExtension(String contentType) {
        switch (contentType) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/webp":
                return ".webp";
            default:
                return ".bin";
        }
    }

    /**
     * Generate public file URL
     */
    private String generateFileUrl(String key) {
        if (cloudFrontDomain != null && !cloudFrontDomain.isEmpty()) {
            // Use CloudFront CDN if configured
            return "https://" + cloudFrontDomain + "/" + key;
        } else {
            // Use direct S3 URL
            return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucketName,
                this.region,
                key
            );
        }
    }

    /**
     * Extract S3 key from public URL
     */
    private String extractKeyFromUrl(String fileUrl) {
        try {
            if (cloudFrontDomain != null && !cloudFrontDomain.isEmpty() && fileUrl.contains(cloudFrontDomain)) {
                // CloudFront URL: https://cdn.example.com/avatars/file.jpg
                return fileUrl.substring(fileUrl.indexOf(cloudFrontDomain) + cloudFrontDomain.length() + 1);
            } else if (fileUrl.contains(".s3.")) {
                // S3 URL: https://bucket.s3.region.amazonaws.com/avatars/file.jpg
                return fileUrl.substring(fileUrl.indexOf(".amazonaws.com/") + 15);
            }
            return null;
        } catch (Exception e) {
            logger.error("Failed to extract key from URL: {}", fileUrl, e);
            return null;
        }
    }
}
