package com.etalente.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDto {
    private UUID id;
    private String url;
    private String filename;
    private LocalDateTime uploadDate;
}
