package com.etalente.backend.service;

import com.etalente.backend.dto.ApplicationDetailsDto;
import com.etalente.backend.dto.ApplicationSummaryDto;
import com.etalente.backend.dto.EmployerApplicationSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface JobApplicationService {

    Page<ApplicationSummaryDto> getMyApplications(Pageable pageable, String search, String sort);

    ApplicationDetailsDto getApplicationDetails(UUID applicationId);

    ApplicationSummaryDto applyForJob(UUID jobId);

    void withdrawApplication(UUID applicationId);

    Page<EmployerApplicationSummaryDto> getApplicationsForJob(UUID jobId, UUID userId, Pageable pageable);
}
