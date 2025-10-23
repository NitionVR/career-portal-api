package com.etalente.backend.dto;

import com.etalente.backend.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationMemberDto {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Role role;
    private String status; // e.g., ACTIVE, DISABLED
    private LocalDateTime joinedDate;
}
