package com.etalente.backend.service.impl;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
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

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostServiceImplTest {

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JobPostServiceImpl jobPostService;

    private Faker faker;
    private User testUser;
    private String userEmail;
    private JobPost testJobPost;
    private UUID jobPostId;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        userEmail = faker.internet().emailAddress();
        jobPostId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail(userEmail);

        testJobPost = new JobPost();
        testJobPost.setId(jobPostId);
        testJobPost.setCreatedBy(testUser);
        testJobPost.setTitle(faker.job().title());
    }

    @Test
    void createJobPost_shouldSaveAndReturnJobPost() {
        // Given
        JobPostRequest request = createFakeJobPostRequest();
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
        when(jobPostRepository.save(any(JobPost.class))).thenReturn(testJobPost);

        // When
        jobPostService.createJobPost(request, userEmail);

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
        JobPostRequest request = createFakeJobPostRequest();
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.of(testJobPost));
        when(jobPostRepository.save(any(JobPost.class))).thenReturn(testJobPost);

        // When
        jobPostService.updateJobPost(jobPostId, request, userEmail);

        // Then
        verify(jobPostRepository).save(any(JobPost.class));
    }

    @Test
    void updateJobPost_shouldThrowUnauthorized_whenNotOwner() {
        // Given
        JobPostRequest request = createFakeJobPostRequest();
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.of(testJobPost));
        String otherUserEmail = faker.internet().emailAddress();

        // When & Then
        assertThrows(UnauthorizedException.class, () -> jobPostService.updateJobPost(jobPostId, request, otherUserEmail));
    }

    @Test
    void deleteJobPost_shouldDeleteSuccessfully_whenOwner() {
        // Given
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.of(testJobPost));

        // When
        jobPostService.deleteJobPost(jobPostId, userEmail);

        // Then
        verify(jobPostRepository).delete(testJobPost);
    }

    @Test
    void deleteJobPost_shouldThrowUnauthorized_whenNotOwner() {
        // Given
        when(jobPostRepository.findById(jobPostId)).thenReturn(Optional.of(testJobPost));
        String otherUserEmail = faker.internet().emailAddress();

        // When & Then
        assertThrows(UnauthorizedException.class, () -> jobPostService.deleteJobPost(jobPostId, otherUserEmail));
    }

    @Test
    void listJobPosts_shouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPost> page = new PageImpl<>(Collections.singletonList(testJobPost));
        when(jobPostRepository.findAll(pageable)).thenReturn(page);

        // When
        var result = jobPostService.listJobPosts(pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(testJobPost.getTitle(), result.getContent().get(0).title());
    }

    @Test
    void listJobPostsByUser_shouldReturnFilteredPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<JobPost> page = new PageImpl<>(Collections.singletonList(testJobPost));
        when(jobPostRepository.findByCreatedByEmail(userEmail, pageable)).thenReturn(page);

        // When
        var result = jobPostService.listJobPostsByUser(userEmail, pageable);

        // Then
        assertEquals(1, result.getTotalElements());
        assertEquals(userEmail, result.getContent().get(0).createdByEmail());
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
