package com.etalente.backend.service.impl;

import com.etalente.backend.dto.BulkActionResponse;
import com.etalente.backend.dto.BulkStatusUpdateRequest;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.model.JobApplicationStatus;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.service.JobApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicantBulkActionsTest {

    @Mock
    private JobApplicationService jobApplicationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ApplicantServiceImpl applicantService;

    @Test
    void bulkUpdateStatus_withValidApplications_shouldUpdateAll() {
        // Given
        List<UUID> appIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        BulkStatusUpdateRequest request = new BulkStatusUpdateRequest();
        request.setApplicationIds(appIds);
        request.setTargetStatus(JobApplicationStatus.UNDER_REVIEW);

        when(userRepository.findById(any())).thenReturn(Optional.of(new User()));

        // When
        BulkActionResponse response = applicantService.bulkUpdateStatus(request, UUID.randomUUID());

        // Then
        assertThat(response.getSuccessCount()).isEqualTo(2);
        assertThat(response.getFailureCount()).isEqualTo(0);
        verify(jobApplicationService, times(2)).transitionApplicationStatus(any(), any(), any());
    }

    @Test
    void bulkUpdateStatus_exceedingMaxSize_shouldThrowException() {
        // Given
        List<UUID> appIds = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            appIds.add(UUID.randomUUID());
        }
        BulkStatusUpdateRequest request = new BulkStatusUpdateRequest();
        request.setApplicationIds(appIds);

        // When & Then
        assertThatThrownBy(() ->
            applicantService.bulkUpdateStatus(request, UUID.randomUUID())
        ).isInstanceOf(BadRequestException.class)
         .hasMessageContaining("Cannot update more than 100 applications");
    }
}
