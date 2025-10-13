package com.etalente.backend.service;

import com.etalente.backend.dto.ApplicationDetailsDto;
import com.etalente.backend.dto.ApplicationSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface JobApplicationService {

    Page<ApplicationSummaryDto> getMyApplications(Pageable pageable, String search);

    ApplicationDetailsDto getApplicationDetails(UUID applicationId);

    ApplicationSummaryDto applyForJob(UUID jobPostId);

    void withdrawApplication(UUID applicationId);
}
