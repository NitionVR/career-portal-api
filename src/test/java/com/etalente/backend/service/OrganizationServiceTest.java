package com.etalente.backend.service;

import com.etalente.backend.dto.OrganizationMemberDto;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private OrganizationService organizationService;

    private UUID organizationId;
    private Organization organization;
    private User hiringManager;
    private User recruiter;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        organization = new Organization();
        organization.setId(organizationId);
        organization.setName("Test Org");

        hiringManager = new User();
        hiringManager.setId(UUID.randomUUID());
        hiringManager.setEmail("hm@test.com");
        hiringManager.setFirstName("Hiring");
        hiringManager.setLastName("Manager");
        hiringManager.setRole(Role.HIRING_MANAGER);
        hiringManager.setOrganization(organization);
        hiringManager.setCreatedAt(LocalDateTime.now());

        recruiter = new User();
        recruiter.setId(UUID.randomUUID());
        recruiter.setEmail("rec@test.com");
        recruiter.setFirstName("Recruiter");
        recruiter.setLastName("User");
        recruiter.setRole(Role.RECRUITER);
        recruiter.setOrganization(organization);
        recruiter.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("getOrganizationMembers should return a list of members for a valid organization ID")
    void getOrganizationMembers_shouldReturnMembersList() {
        // Given
        when(userRepository.findByOrganizationId(organizationId))
                .thenReturn(List.of(hiringManager, recruiter));

        // When
        List<OrganizationMemberDto> members = organizationService.getOrganizationMembers(organizationId);

        // Then
        assertThat(members).isNotNull().hasSize(2);
        assertThat(members.get(0).getEmail()).isEqualTo(hiringManager.getEmail());
        assertThat(members.get(1).getEmail()).isEqualTo(recruiter.getEmail());
    }

    @Test
    @DisplayName("getOrganizationMembers should return an empty list if no members found")
    void getOrganizationMembers_shouldReturnEmptyList_whenNoMembers() {
        // Given
        when(userRepository.findByOrganizationId(organizationId))
                .thenReturn(List.of());

        // When
        List<OrganizationMemberDto> members = organizationService.getOrganizationMembers(organizationId);

        // Then
        assertThat(members).isNotNull().isEmpty();
    }
}
