package com.etalente.backend.service;

import com.etalente.backend.dto.ApplicantSummaryDto;
import com.etalente.backend.dto.BulkActionResponse;
import com.etalente.backend.dto.BulkStatusUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ApplicantService {
    Page<ApplicantSummaryDto> getApplicants(Pageable pageable,
                                            String search,
                                            String skillSearch,
                                            String jobId,
                                            List<String> statuses,
                                            Integer experienceMin,
                                            List<String> education,
                                            String location,
                                            Integer aiMatchScoreMin,
                                            UUID organizationId);

    BulkActionResponse bulkUpdateStatus(BulkStatusUpdateRequest request, UUID userId);
}
