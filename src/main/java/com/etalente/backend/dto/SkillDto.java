package com.etalente.backend.dto;

import java.util.List;

public record SkillDto(
        String name,
        String level,
        List<String> keywords
) {}