package com.etalente.backend.service;

import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JobPostStateMachineEdgeCasesTest {

    @Autowired
    private JobPostStateMachine stateMachine;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private Organization testOrganization;
    private JobPost testJobPost;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setName("Edge Case Company " + UUID.randomUUID());
        testOrganization = organizationRepository.save(testOrganization);

        testUser = new User();
        testUser.setEmail("edge-case-" + UUID.randomUUID() + "@test.com");
        testUser.setRole(Role.HIRING_MANAGER);
        testUser.setOrganization(testOrganization);
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        testJobPost = createCompleteJobPost();
        testJobPost = jobPostRepository.save(testJobPost);
    }

    @Test
    void testPublishJobPost_MissingAllFields() {
        JobPost incompleteJob = new JobPost();
        incompleteJob.setStatus(JobPostStatus.DRAFT);
        incompleteJob.setCreatedBy(testUser);
        incompleteJob.setOrganization(testOrganization);
        incompleteJob = jobPostRepository.save(incompleteJob);

        JobPost finalJob = incompleteJob;
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> stateMachine.transitionState(
                        finalJob,
                        JobPostStatus.OPEN,
                        testUser,
                        "Trying to publish"
                )
        );

        assertTrue(exception.getMessage().contains("missing required fields"));
    }

    @Test
    void testTransitionFromArchived_Fails() {
        testJobPost.setStatus(JobPostStatus.ARCHIVED);
        testJobPost = jobPostRepository.save(testJobPost);

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
    }

    @Test
    void testReopenClosedJobPost_Success() {
        testJobPost.setStatus(JobPostStatus.CLOSED);
        testJobPost = jobPostRepository.save(testJobPost);

        stateMachine.transitionState(testJobPost, JobPostStatus.OPEN, testUser, "Reopening");

        assertEquals(JobPostStatus.OPEN, testJobPost.getStatus());
    }

    @Test
    void testCloseOpenJobPost_Success() {
        testJobPost.setStatus(JobPostStatus.OPEN);
        testJobPost = jobPostRepository.save(testJobPost);

        stateMachine.transitionState(testJobPost, JobPostStatus.CLOSED, testUser, "Closing");

        assertEquals(JobPostStatus.CLOSED, testJobPost.getStatus());
    }

    @Test
    void testTransitionToSameStatus_Fails() {
        testJobPost.setStatus(JobPostStatus.DRAFT);
        testJobPost = jobPostRepository.save(testJobPost);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> stateMachine.transitionState(
                        testJobPost,
                        JobPostStatus.DRAFT,
                        testUser,
                        "Trying to transition to same status"
                )
        );

        assertTrue(exception.getMessage().contains("already in DRAFT status"));
    }

    private JobPost createCompleteJobPost() {
        JobPost jobPost = new JobPost();
        jobPost.setTitle("Software Engineer");
        jobPost.setDescription("Test description for a software engineer position");
        jobPost.setJobType("Full-time");
        jobPost.setExperienceLevel("Mid-level");
        jobPost.setCompany(testOrganization.getName());

        ObjectNode location = objectMapper.createObjectNode();
        location.put("city", "San Francisco");
        location.put("countryCode", "US");
        jobPost.setLocation(location);
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost.setCreatedBy(testUser);
        jobPost.setOrganization(testOrganization);
        return jobPost;
    }
}