package com.etalente.backend.dto;

import com.etalente.backend.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
    @NotNull Role newRole
) {}
