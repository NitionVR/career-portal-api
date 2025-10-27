package com.etalente.backend.integration.documentparser;

import com.etalente.backend.service.S3Service;
import com.etalente.backend.exception.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
public class DocumentParserClient {

    private final RestClient restClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final S3Service s3Service;
    private final String baseUrl;

    public DocumentParserClient(@Value("${document-parser.base-url}") String baseUrl,
                                ObjectMapper objectMapper,
                                S3Service s3Service) {
        this.baseUrl = baseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.s3Service = s3Service;
    }

    public JsonNode extractResume(String resumeS3Url) {
        log.info("Calling document parser to extract resume from: {}", resumeS3Url);
        try {
            // 1. Download the file from S3
            byte[] fileContent = s3Service.downloadFile(resumeS3Url);
            log.info("Downloaded file content size: {} bytes", fileContent.length);

            // 2. Create a Multipart request body
            LinkedMultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource resource = new ByteArrayResource(fileContent) {
                @Override
                public String getFilename() {
                    String[] parts = resumeS3Url.split("/");
                    String filename = parts[parts.length - 1];
                    log.info("Multipart filename: {}", filename);
                    return filename;
                }
            };

            body.add("file", resource); // lowercase "file"

            // 3. Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity =
                new HttpEntity<>(body, headers);

            // 4. Send the multipart request using RestTemplate
            log.info("Sending multipart request to document parser");
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                baseUrl + "/extract/",
                requestEntity,
                JsonNode.class
            );

            log.info("Successfully extracted resume from document parser. Status: {}", response.getStatusCode());
            return response.getBody();

        } catch (Exception e) {
            log.error("Failed to extract resume from document parser for URL: {}", resumeS3Url, e);
            throw new ServiceException("Failed to extract resume data", e);
        }
    }

    public JsonNode extractJobPost(String jobS3Url) {
        log.info("Calling document parser to extract job post from: {}", jobS3Url);
        try {
            Map<String, String> requestBody = Map.of("s3_url", jobS3Url);
            JsonNode response = restClient.post()
                    .uri("/job-post-extract")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
            log.info("Successfully extracted job post from document parser.");
            return response;
        } catch (Exception e) {
            log.error("Failed to extract job post from document parser for URL: {}", jobS3Url, e);
            throw new ServiceException("Failed to extract job post data", e);
        }
    }
}

