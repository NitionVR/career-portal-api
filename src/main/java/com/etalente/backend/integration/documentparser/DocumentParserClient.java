package com.etalente.backend.integration.documentparser;

import com.etalente.backend.exception.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
public class DocumentParserClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DocumentParserClient(@Value("${document-parser.base-url}") String baseUrl, ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    public JsonNode extractResume(String resumeS3Url) {
        log.info("Calling document parser to extract resume from: {}", resumeS3Url);
        try {
            Map<String, String> requestBody = Map.of("s3_url", resumeS3Url);
            JsonNode response = restClient.post()
                    .uri("/extract")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
            log.info("Successfully extracted resume from document parser.");
            return response;
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
