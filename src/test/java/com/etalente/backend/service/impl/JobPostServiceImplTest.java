package com.etalente.backend.service.impl;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.JobPostPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class JobPostServiceImplTest {

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationContext organizationContext;

    @Mock  // ADD THIS
    private JobPostPermissionService permissionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JobPostServiceImpl jobPostService;

    private Faker faker;
    private User testUser;
    private Organization organization;
    private UUID userId;
    private JobPost testJobPost;
    private UUID jobPostId;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        userId = UUID.randomUUID();
        jobPostId = UUID.randomUUID();

        organization = new Organization();
        organization.setId(UUID.randomUUID());
        organization.setName(faker.company().name());

        testUser = new User();
        testUser.setId(userId);
        testUser.setOrganization(organization);

        testJobPost = new JobPost();
        testJobPost.setId(jobPostId);
        testJobPost.setCreatedBy(testUser);
        testJobPost.setOrganization(organization);
        testJobPost.setTitle(faker.job().title());
    }

    @Test
    void createJobPost_shouldSaveAndReturnJobPost() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(organizationContext.requireOrganization()).thenReturn(organization);
        JobPostRequest request = createFakeJobPostRequest();
        when(jobPostRepository.save(any(JobPost.class))).thenReturn(testJobPost);

        // ADD THIS - mock permission check
        doNothing().when(permissionService).verifyCanCreate(any(User.class));

        // When
        jobPostService.createJobPost(request, userId);

        // Then
        ArgumentCaptor<JobPost> jobPostCaptor = ArgumentCaptor.forClass(JobPost.class);
        verify(jobPostRepository).save(jobPostCaptor.capture());
        JobPost savedJobPost = jobPostCaptor.getValue();

        assertEquals(request.title(), savedJobPost.getTitle());
        assertEquals(testUser, savedJobPost.getCreatedBy());
        assertNotNull(savedJobPost.getDatePosted());
    }

    @Test
    void getJobPost_shouldReturnJobPost_whenFound() {
        // Given
        testJobPost.setStatus(JobPostStatus.OPEN);
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.of(testJobPost));

        // When
        var response = jobPostService.getJobPost(jobPostId);

        // Then
        assertNotNull(response);
        assertEquals(testJobPost.getTitle(), response.title());
    }

    @Test
    void getJobPost_shouldThrowResourceNotFound_whenNotFound() {
        // Given
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> jobPostService.getJobPost(jobPostId));
    }

    @Test
    void updateJobPost_shouldUpdateSuccessfully_whenOwner() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(organizationContext.requireOrganization()).thenReturn(organization);
        JobPostRequest request = createFakeJobPostRequest();
        when(jobPostRepository.findByIdAndOrganization(jobPostId, organization)).thenReturn(Optional.of(testJobPost));
        when(jobPostRepository.save(any(JobPost.class))).thenReturn(testJobPost);
        doNothing().when(permissionService).verifyCanUpdate(any(User.class), any(JobPost.class)); // ADD THIS

        // When
        jobPostService.updateJobPost(jobPostId, request, userId);

        // Then
        verify(jobPostRepository).save(any(JobPost.class));
    }

    @Test
    void updateJobPost_shouldThrowUnauthorized_whenNotOwner() {
        // Given
        when(organizationContext.requireOrganization()).thenReturn(organization);
        JobPostRequest request = createFakeJobPostRequest();
        when(jobPostRepository.findByIdAndOrganization(jobPostId, organization)).thenReturn(Optional.of(testJobPost));
        UUID otherUserId = UUID.randomUUID();

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setOrganization(organization);
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        // Mock permission service to throw
        doThrow(new UnauthorizedException("Not authorized")).when(permissionService)
                .verifyCanUpdate(any(User.class), any(JobPost.class));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> jobPostService.updateJobPost(jobPostId, request, otherUserId));
    }

    @Test
    void deleteJobPost_shouldDeleteSuccessfully_whenOwner() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(organizationContext.requireOrganization()).thenReturn(organization);
        when(jobPostRepository.findByIdAndOrganization(jobPostId, organization)).thenReturn(Optional.of(testJobPost));
        doNothing().when(permissionService).verifyCanDelete(any(User.class), any(JobPost.class)); // ADD THIS

        // When
        jobPostService.deleteJobPost(jobPostId, userId);

        // Then
        verify(jobPostRepository).delete(testJobPost);
    }

    @Test
    void deleteJobPost_shouldThrowUnauthorized_whenNotOwner() {
        // Given
        when(organizationContext.requireOrganization()).thenReturn(organization);
        when(jobPostRepository.findByIdAndOrganization(jobPostId, organization)).thenReturn(Optional.of(testJobPost));
        UUID otherUserId = UUID.randomUUID();

        User otherUser = new User();
        otherUser.setId(otherUserId);
        otherUser.setOrganization(organization);
        when(userRepository.findById(otherUserId)).thenReturn(Optional.of(otherUser));

        // Mock permission service to throw
        doThrow(new UnauthorizedException("Not authorized")).when(permissionService)
                .verifyCanDelete(any(User.class), any(JobPost.class));

        // When & Then
        assertThrows(UnauthorizedException.class, () -> jobPostService.deleteJobPost(jobPostId, otherUserId));
    }

    @Test
    void listJobPosts_shouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPost> page = new PageImpl<>(Collections.singletonList(testJobPost));
        User candidateUser = new User();
        candidateUser.setRole(Role.CANDIDATE);
        when(organizationContext.getCurrentUserOrNull()).thenReturn(candidateUser);
        when(jobPostRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        // When
        var result = jobPostService.listJobPosts(pageable, null, null, null, null, null);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(testJobPost.getTitle(), result.getContent().get(0).title());
    }

    @Test
    void listJobPostsByUser_shouldReturnFilteredPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPost> page = new PageImpl<>(Collections.singletonList(testJobPost));
        when(organizationContext.requireOrganization()).thenReturn(organization);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(jobPostRepository.findByCreatedByIdAndOrganization(userId, organization, pageable)).thenReturn(page);

        // When
        var result = jobPostService.listJobPostsByUser(userId, pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(testUser.getEmail(), result.getContent().get(0).createdByEmail());
    }

    private JobPostRequest createFakeJobPostRequest() {
        return new JobPostRequest(
                faker.job().title(),
                faker.company().name(),
                "Full-time",
                faker.lorem().paragraph(),
                null,
                "Remote",
                "100-200k",
                "Mid-level",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}