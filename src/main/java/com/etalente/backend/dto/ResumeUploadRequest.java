package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUploadRequest {

    @NotBlank(message = "Content type cannot be empty")
    private String contentType;

    @Positive(message = "Content length must be positive")
    private long contentLength;

    @NotBlank(message = "File name cannot be empty")
    @Size(max = 255, message = "File name too long")
    private String fileName;
}
