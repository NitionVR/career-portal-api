package com.etalente.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionResponse {
    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<BulkActionError> errors;
    private LocalDateTime processedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkActionError {
        private UUID applicationId;
        private String error;
        private String reason;
    }
}
