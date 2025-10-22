package com.etalente.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private String message;
    private List<String> errors;
    private int status;
    private LocalDateTime timestamp;
    private String path;
    private Map<String, Object> details;

    public ErrorResponse(String message, List<String> errors, int status) {
        this.message = message;
        this.errors = errors;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
}
