package com.etalente.backend.service;

import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStateAudit;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.StateTransition;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.JobPostStateAuditRepository;
import com.etalente.backend.service.impl.JobPostStateMachineImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobPostStateMachineTest {

    @Mock
    private JobPostRepository jobPostRepository;

    @Mock
    private JobPostStateAuditRepository stateAuditRepository;

    @InjectMocks
    private JobPostStateMachineImpl stateMachine;

    private ObjectMapper objectMapper;
    private User testUser;
    private Organization testOrganization;
    private JobPost testJobPost;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        testOrganization = new Organization();
        testOrganization.setId(UUID.randomUUID());
        testOrganization.setName("Test Company");

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.HIRING_MANAGER);
        testUser.setOrganization(testOrganization);

        testJobPost = createCompleteJobPost();
    }

    @Test
    void testValidTransition_DraftToOpen() {
        testJobPost.setStatus(JobPostStatus.DRAFT);

        when(jobPostRepository.save(any(JobPost.class))).thenReturn(testJobPost);

        JobPost result = stateMachine.transitionState(
                testJobPost,
                JobPostStatus.OPEN,
                testUser,
                "Publishing job"
        );

        assertEquals(JobPostStatus.OPEN, result.getStatus());
        verify(jobPostRepository).save(testJobPost);
        verify(stateAuditRepository).save(any(JobPostStateAudit.class));
    }

    @Test
    void testValidTransition_OpenToClosed() {
        testJobPost.setStatus(JobPostStatus.OPEN);

        when(jobPostRepository.save(any(JobPost.class))).thenReturn(testJobPost);

        JobPost result = stateMachine.transitionState(
                testJobPost,
                JobPostStatus.CLOSED,
                testUser,
                "Position filled"
        );

        assertEquals(JobPostStatus.CLOSED, result.getStatus());
        verify(stateAuditRepository).save(any(JobPostStateAudit.class));
    }

    @Test
    void testValidTransition_ClosedToOpen() {
        testJobPost.setStatus(JobPostStatus.CLOSED);

        when(jobPostRepository.save(any(JobPost.class))).thenReturn(testJobPost);

        JobPost result = stateMachine.transitionState(
                testJobPost,
                JobPostStatus.OPEN,
                testUser,
                "Reopening position"
        );

        assertEquals(JobPostStatus.OPEN, result.getStatus());
        verify(stateAuditRepository).save(any(JobPostStateAudit.class));
    }

    @Test
    void testInvalidTransition_ArchivedToOpen() {
        testJobPost.setStatus(JobPostStatus.ARCHIVED);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> stateMachine.transitionState(
                        testJobPost,
                        JobPostStatus.OPEN,
                        testUser,
                        "Trying to reopen archived"
                )
        );

        assertTrue(exception.getMessage().contains("Invalid state transition"));
        verify(jobPostRepository, never()).save(any());
        verify(stateAuditRepository, never()).save(any());
    }

    @Test
    void testInvalidTransition_DraftToClosed() {
        testJobPost.setStatus(JobPostStatus.DRAFT);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> stateMachine.transitionState(
                        testJobPost,
                        JobPostStatus.CLOSED,
                        testUser,
                        "Invalid transition"
                )
        );

        assertTrue(exception.getMessage().contains("Invalid state transition"));
    }

    @Test
    void testTransitionToSameStatus_ThrowsException() {
        testJobPost.setStatus(JobPostStatus.OPEN);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> stateMachine.transitionState(
                        testJobPost,
                        JobPostStatus.OPEN,
                        testUser,
                        "Same status"
                )
        );

        assertTrue(exception.getMessage().contains("already in OPEN status"));
    }

    @Test
    void testPublishIncompleteJobPost_ThrowsException() {
        testJobPost.setStatus(JobPostStatus.DRAFT);
        testJobPost.setTitle(null); // Make it incomplete

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> stateMachine.transitionState(
                        testJobPost,
                        JobPostStatus.OPEN,
                        testUser,
                        "Trying to publish incomplete"
                )
        );

        assertTrue(exception.getMessage().contains("missing required fields"));
    }

    @Test
    void testCanBePublished_CompleteJobPost() {
        assertTrue(stateMachine.canBePublished(testJobPost));
    }

    @Test
    void testCanBePublished_MissingTitle() {
        testJobPost.setTitle(null);
        assertFalse(stateMachine.canBePublished(testJobPost));
    }

    @Test
    void testCanBePublished_EmptyDescription() {
        testJobPost.setDescription("");
        assertFalse(stateMachine.canBePublished(testJobPost));
    }

    @Test
    void testAuditRecordCreation() {
        testJobPost.setStatus(JobPostStatus.DRAFT);
        when(jobPostRepository.save(any(JobPost.class))).thenReturn(testJobPost);

        stateMachine.transitionState(
                testJobPost,
                JobPostStatus.OPEN,
                testUser,
                "Test reason"
        );

        ArgumentCaptor<JobPostStateAudit> auditCaptor = ArgumentCaptor.forClass(JobPostStateAudit.class);
        verify(stateAuditRepository).save(auditCaptor.capture());

        JobPostStateAudit audit = auditCaptor.getValue();
        assertEquals(JobPostStatus.DRAFT, audit.getFromStatus());
        assertEquals(JobPostStatus.OPEN, audit.getToStatus());
        assertEquals(testUser, audit.getChangedBy());
        assertEquals("Test reason", audit.getReason());
        assertEquals(testJobPost, audit.getJobPost());
    }

    @Test
    void testAllValidTransitions() {
        // Test all valid transitions defined in StateTransition enum
        testValidTransitionPair(JobPostStatus.DRAFT, JobPostStatus.OPEN);
        testValidTransitionPair(JobPostStatus.DRAFT, JobPostStatus.ARCHIVED);
        testValidTransitionPair(JobPostStatus.OPEN, JobPostStatus.CLOSED);
        testValidTransitionPair(JobPostStatus.OPEN, JobPostStatus.DRAFT);
        testValidTransitionPair(JobPostStatus.OPEN, JobPostStatus.ARCHIVED);
        testValidTransitionPair(JobPostStatus.CLOSED, JobPostStatus.OPEN);
        testValidTransitionPair(JobPostStatus.CLOSED, JobPostStatus.ARCHIVED);
    }

    private void testValidTransitionPair(JobPostStatus from, JobPostStatus to) {
        assertTrue(StateTransition.isValidTransition(from, to),
                String.format("Transition from %s to %s should be valid", from, to));
    }

    private JobPost createCompleteJobPost() {
        JobPost jobPost = new JobPost();
        jobPost.setId(UUID.randomUUID());
        jobPost.setTitle("Software Engineer");
        jobPost.setDescription("Test description");
        jobPost.setJobType("Full-time");
        jobPost.setExperienceLevel("Mid-level");
        jobPost.setLocation(objectMapper.valueToTree(new LocationDto("123 Main St", "12345", "San Francisco", "US", "CA")));
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost.setCreatedBy(testUser);
        jobPost.setOrganization(testOrganization);
        return jobPost;
    }
}