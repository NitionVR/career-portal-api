package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.config.UploadProperties;
import com.etalente.backend.dto.UploadUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class S3ServiceTest extends BaseIntegrationTest {

    @Autowired
    private S3Service s3Service;

    @Autowired
    private S3Presigner s3Presigner; // This is the mock from TestAwsConfig

    @MockBean
    private UploadProperties uploadProperties;

    @BeforeEach
    void setUp() {
        when(uploadProperties.getAllowedContentTypes()).thenReturn(List.of("image/png"));
        when(uploadProperties.getMaxFileSize()).thenReturn(5242880L); // 5MB
        when(uploadProperties.getPresignedUrlExpirationMinutes()).thenReturn(15);
    }

    @Test
    @DisplayName("generatePresignedUploadUrl should create a URL without metadata or content-length")
    void generatePresignedUploadUrl_shouldGenerateUrlWithoutMetadata() throws Exception {
        // Given
        String folder = "avatars";
        String contentType = "image/png";
        long contentLength = 1024L;
        String expectedUrl = "https://s3.amazonaws.com/test-bucket/avatars/test.png?presigned-url-stuff";

        // Mock the S3Presigner response
        PresignedPutObjectRequest mockPresignedRequest = Mockito.mock(PresignedPutObjectRequest.class);
        when(mockPresignedRequest.url()).thenReturn(new URL(expectedUrl));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(mockPresignedRequest);

        // When
        UploadUrlResponse response = s3Service.generatePresignedUploadUrl(folder, contentType, contentLength);

        // Then
        assertThat(response.getUploadUrl()).isEqualTo(expectedUrl);

        // Capture the request passed to the presigner
        ArgumentCaptor<PutObjectPresignRequest> presignRequestCaptor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        Mockito.verify(s3Presigner).presignPutObject(presignRequestCaptor.capture());
        PutObjectPresignRequest capturedPresignRequest = presignRequestCaptor.getValue();

        // Verify the underlying PutObjectRequest
        PutObjectRequest capturedPutRequest = capturedPresignRequest.putObjectRequest();
        assertThat(capturedPutRequest.metadata()).isEmpty();

        // Also assert that the generated URL does not contain headers that were causing issues
        assertThat(response.getUploadUrl()).doesNotContain("x-amz-meta-");
    }
}
