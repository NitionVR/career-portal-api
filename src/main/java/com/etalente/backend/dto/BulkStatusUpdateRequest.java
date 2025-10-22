package com.etalente.backend.dto;

import com.etalente.backend.model.JobApplicationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkStatusUpdateRequest {

    @NotEmpty(message = "Application IDs cannot be empty")
    @Size(max = 100, message = "Cannot update more than 100 applications at once")
    private List<UUID> applicationIds;

    @NotNull(message = "Target status is required")
    private JobApplicationStatus targetStatus;

    private boolean sendNotification = false;

    private String note; // Optional note about the status change
}
