package com.etalente.backend.service.impl;

import com.etalente.backend.constants.JsonFieldConstants;
import com.etalente.backend.dto.ApplicantSummaryDto;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ServiceException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobApplicationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicantServiceImplTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @InjectMocks
    private ApplicantServiceImpl applicantService;

    private ObjectMapper objectMapper;
    private UUID organizationId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        organizationId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "applicationDate"));
    }

    @Nested
    class ValidationTests {

        @Test
        void getApplicants_withNullOrganizationId_shouldThrowBadRequestException() {
            // When & Then
            assertThatThrownBy(() ->
                applicantService.getApplicants(
                    pageable, null, null, null, null,
                    null, null, null, null, null
                )
            ).isInstanceOf(BadRequestException.class)
             .hasMessageContaining("Organization ID is required");
        }

        @Test
        void getApplicants_withSearchTermTooLong_shouldThrowBadRequestException() {
            // Given
            String longSearch = "a".repeat(101);

            // When & Then
            assertThatThrownBy(() ->
                applicantService.getApplicants(
                    pageable, longSearch, null, null, null,
                    null, null, null, null, organizationId
                )
            ).isInstanceOf(BadRequestException.class)
             .hasMessageContaining("Search term too long");
        }

        @Test
        void getApplicants_withInvalidExperienceMin_shouldThrowBadRequestException() {
            // When & Then
            assertThatThrownBy(() ->
                applicantService.getApplicants(
                    pageable, null, null, null, null,
                    -1, null, null, null, organizationId
                )
            ).isInstanceOf(BadRequestException.class)
             .hasMessageContaining("Experience minimum must be between");
        }

        @Test
        void getApplicants_withInvalidJobId_shouldThrowBadRequestException() {
            // When & Then
            assertThatThrownBy(() ->
                applicantService.getApplicants(
                    pageable, null, null, "invalid-uuid", null,
                    null, null, null, null, organizationId
                )
            ).isInstanceOf(BadRequestException.class)
             .hasMessageContaining("Invalid jobId format");
        }
    }

    @Nested
    class FilteringTests {

        @Test
        void getApplicants_withNoFilters_shouldReturnAllOrgApplications() {
            // Given
            List<JobApplication> applications = createMockApplications(5);
            Page<JobApplication> applicationPage = new PageImpl<>(applications, pageable, 5);

            when(jobApplicationRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(applicationPage);

            // When
            Page<ApplicantSummaryDto> result = applicantService.getApplicants(
                pageable, null, null, null, null,
                null, null, null, null, organizationId
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getContent()).hasSize(5);
            verify(jobApplicationRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        void getApplicants_withSearchTerm_shouldFilterByNameAndJobTitle() {
            // Given
            String search = "java";
            List<JobApplication> applications = createMockApplications(2);
            Page<JobApplication> applicationPage = new PageImpl<>(applications, pageable, 2);

            when(jobApplicationRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(applicationPage);

            // When
            Page<ApplicantSummaryDto> result = applicantService.getApplicants(
                pageable, search, null, null, null,
                null, null, null, null, organizationId
            );

            // Then
            assertThat(result).isNotNull();
            verify(jobApplicationRepository).findAll(any(Specification.class), eq(pageable));
        }

        @Test
        void getApplicants_withJobIdFilter_shouldFilterByJobPost() {
            // Given
            UUID jobId = UUID.randomUUID();
            List<JobApplication> applications = createMockApplications(3);
            Page<JobApplication> applicationPage = new PageImpl<>(applications, pageable, 3);

            when(jobApplicationRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(applicationPage);

            // When
            Page<ApplicantSummaryDto> result = applicantService.getApplicants(
                pageable, null, null, jobId.toString(), null,
                null, null, null, null, organizationId
            );

            // Then
            assertThat(result).isNotNull();
            verify(jobApplicationRepository).findAll(any(Specification.class), eq(pageable));
        }
    }

    // Helper methods
    private List<JobApplication> createMockApplications(int count) {
        List<JobApplication> applications = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            applications.add(createMockJobApplication(i));
        }
        return applications;
    }

    private JobApplication createMockJobApplication(int index) {
        JobApplication application = new JobApplication();
        application.setId(UUID.randomUUID());
        application.setApplicationDate(LocalDateTime.now().minusDays(index));
        application.setStatus(JobApplicationStatus.APPLIED);

        User candidate = new User();
        candidate.setId(UUID.randomUUID());
        candidate.setFirstName("John" + index);
        candidate.setLastName("Doe" + index);
        candidate.setProfileImageUrl("http://example.com/profile" + index + ".jpg");

        JobPost jobPost = new JobPost();
        jobPost.setId(UUID.randomUUID());
        jobPost.setTitle("Software Engineer " + index);

        Organization org = new Organization();
        org.setId(organizationId);
        jobPost.setOrganization(org);

        application.setCandidate(candidate);
        application.setJobPost(jobPost);

        return application;
    }
}
