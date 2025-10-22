package com.etalente.backend.dto;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class PageApplicantSummaryDto extends PageImpl<ApplicantSummaryDto> {
    public PageApplicantSummaryDto(List<ApplicantSummaryDto> content, Pageable pageable, long total) {
        super(content, pageable, total);
    }
}
