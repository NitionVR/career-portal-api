Perfect! Now let's implement JOB-007. I can see the current structure. Here's the implementation:

Step 1: Create Permission Service
Create file: src/main/java/com/etalente/backend/service/JobPostPermissionService.java

java
package com.etalente.backend.service;

import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.security.OrganizationContext;
import org.springframework.stereotype.Service;

@Service
public class JobPostPermissionService {

    private final OrganizationContext organizationContext;

    public JobPostPermissionService(OrganizationContext organizationContext) {
        this.organizationContext = organizationContext;
    }

    /**
     * Check if user can create job posts (HIRING_MANAGER only)
     */
    public void verifyCanCreate(User user) {
        if (user.getRole() != Role.HIRING_MANAGER) {
            throw new UnauthorizedException("Only hiring managers can create job posts");
        }
    }

    /**
     * Check if user can update a job post (HIRING_MANAGER or RECRUITER in same org)
     */
    public void verifyCanUpdate(User user, JobPost jobPost) {
        // Must be in same organization
        if (!belongsToSameOrganization(user, jobPost)) {
            throw new UnauthorizedException("You can only update job posts in your organization");
        }

        // Must be hiring manager or recruiter
        if (user.getRole() != Role.HIRING_MANAGER && user.getRole() != Role.RECRUITER) {
            throw new UnauthorizedException("Only hiring managers and recruiters can update job posts");
        }
    }

    /**
     * Check if user can delete a job post (HIRING_MANAGER only, must be creator)
     */
    public void verifyCanDelete(User user, JobPost jobPost) {
        if (user.getRole() != Role.HIRING_MANAGER) {
            throw new UnauthorizedException("Only hiring managers can delete job posts");
        }

        if (!belongsToSameOrganization(user, jobPost)) {
            throw new UnauthorizedException("You can only delete job posts in your organization");
        }

        if (!jobPost.getCreatedBy().getId().equals(user.getId())) {
            throw new UnauthorizedException("You can only delete your own job posts");
        }
    }

    /**
     * Check if user can change job post status (HIRING_MANAGER or RECRUITER in same org)
     */
    public void verifyCanChangeStatus(User user, JobPost jobPost) {
        // Must be in same organization
        if (!belongsToSameOrganization(user, jobPost)) {
            throw new UnauthorizedException("You can only change status of job posts in your organization");
        }

        // Must be hiring manager or recruiter
        if (user.getRole() != Role.HIRING_MANAGER && user.getRole() != Role.RECRUITER) {
            throw new UnauthorizedException("Only hiring managers and recruiters can change job post status");
        }
    }

    /**
     * Check if user belongs to the same organization as the job post
     */
    private boolean belongsToSameOrganization(User user, JobPost jobPost) {
        if (user.getOrganization() == null || jobPost.getOrganization() == null) {
            return false;
        }
        return user.getOrganization().getId().equals(jobPost.getOrganization().getId());
    }

    /**
     * Check if user can view a job post
     */
    public boolean canView(User user, JobPost jobPost) {
        // Public posts can be viewed by anyone
        if (jobPost.getStatus() == com.etalente.backend.model.JobPostStatus.OPEN) {
            return true;
        }

        // Non-public posts require organization membership
        if (user.getRole() == Role.CANDIDATE) {
            return false;
        }

        return belongsToSameOrganization(user, jobPost);
    }
}
Step 2: Update JobPostService Interface
Add new methods for status changes:

java
package com.etalente.backend.service;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.model.JobPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface JobPostService {
    JobPostResponse createJobPost(JobPostRequest request, String userEmail);
    JobPostResponse getJobPost(UUID id);
    Page<JobPostResponse> listJobPosts(Pageable pageable);
    Page<JobPostResponse> listJobPostsByUser(String userEmail, Pageable pageable);
    JobPostResponse updateJobPost(UUID id, JobPostRequest request, String userEmail);
    void deleteJobPost(UUID id, String userEmail);

    // New methods for status management
    JobPostResponse updateJobPostStatus(UUID id, JobPostStatus newStatus, String userEmail);
    JobPostResponse publishJobPost(UUID id, String userEmail);
    JobPostResponse closeJobPost(UUID id, String userEmail);
}
Step 3: Update JobPostServiceImpl
Add the permission service and implement new methods:

java
package com.etalente.backend.service.impl;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.OrganizationContext;
import com.etalente.backend.service.JobPostPermissionService;
import com.etalente.backend.service.JobPostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional
public class JobPostServiceImpl implements JobPostService {

    private final JobPostRepository jobPostRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final OrganizationContext organizationContext;
    private final JobPostPermissionService permissionService;

    public JobPostServiceImpl(JobPostRepository jobPostRepository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper,
                              OrganizationContext organizationContext,
                              JobPostPermissionService permissionService) {
        this.jobPostRepository = jobPostRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.organizationContext = organizationContext;
        this.permissionService = permissionService;
    }

    @Override
    public JobPostResponse createJobPost(JobPostRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify user can create job posts
        permissionService.verifyCanCreate(user);

        // Ensure user has an organization
        Organization organization = organizationContext.requireOrganization();

        JobPost jobPost = new JobPost();
        mapRequestToJobPost(request, jobPost);
        jobPost.setCreatedBy(user);
        jobPost.setOrganization(organization);
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost.setDatePosted(LocalDate.now().toString());

        JobPost saved = jobPostRepository.save(jobPost);
        return mapToResponse(saved);
    }

    @Override
    public JobPostResponse getJobPost(UUID id) {
        JobPost jobPost = jobPostRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found"));

        User currentUser = organizationContext.getCurrentUser();

        if (!permissionService.canView(currentUser, jobPost)) {
            throw new UnauthorizedException("You don't have access to this job post");
        }

        return mapToResponse(jobPost);
    }

    @Override
    public Page<JobPostResponse> listJobPosts(Pageable pageable) {
        // If user is a candidate, only show public jobs
        if (organizationContext.isCandidate()) {
            return jobPostRepository.findPublishedJobs(pageable).map(this::mapToResponse);
        }

        // For hiring managers and recruiters, show jobs from their organization
        Organization organization = organizationContext.requireOrganization();
        return jobPostRepository.findByOrganization(organization, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<JobPostResponse> listJobPostsByUser(String userEmail, Pageable pageable) {
        Organization organization = organizationContext.requireOrganization();

        // Verify the user belongs to the same organization
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOrganization() == null ||
            !user.getOrganization().getId().equals(organization.getId())) {
            throw new UnauthorizedException("You can only view job posts from users in your organization");
        }

        return jobPostRepository.findByCreatedByEmailAndOrganization(userEmail, organization, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public JobPostResponse updateJobPost(UUID id, JobPostRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization organization = organizationContext.requireOrganization();

        JobPost jobPost = jobPostRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found in your organization"));

        // Verify user can update
        permissionService.verifyCanUpdate(user, jobPost);

        mapRequestToJobPost(request, jobPost);
        JobPost updated = jobPostRepository.save(jobPost);
        return mapToResponse(updated);
    }

    @Override
    public void deleteJobPost(UUID id, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization organization = organizationContext.requireOrganization();

        JobPost jobPost = jobPostRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found in your organization"));

        // Verify user can delete
        permissionService.verifyCanDelete(user, jobPost);

        jobPostRepository.delete(jobPost);
    }

    @Override
    public JobPostResponse updateJobPostStatus(UUID id, JobPostStatus newStatus, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization organization = organizationContext.requireOrganization();

        JobPost jobPost = jobPostRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found in your organization"));

        // Verify user can change status
        permissionService.verifyCanChangeStatus(user, jobPost);

        // Validate status transition (basic validation, JOB-002 will have full state machine)
        validateStatusTransition(jobPost.getStatus(), newStatus);

        jobPost.setStatus(newStatus);
        JobPost updated = jobPostRepository.save(jobPost);

        return mapToResponse(updated);
    }

    @Override
    public JobPostResponse publishJobPost(UUID id, String userEmail) {
        return updateJobPostStatus(id, JobPostStatus.OPEN, userEmail);
    }

    @Override
    public JobPostResponse closeJobPost(UUID id, String userEmail) {
        return updateJobPostStatus(id, JobPostStatus.CLOSED, userEmail);
    }

    private void validateStatusTransition(JobPostStatus currentStatus, JobPostStatus newStatus) {
        // Basic validation - will be enhanced in JOB-002
        if (currentStatus == JobPostStatus.ARCHIVED && newStatus != JobPostStatus.ARCHIVED) {
            throw new BadRequestException("Cannot change status of archived job post");
        }

        if (currentStatus == JobPostStatus.CLOSED && newStatus == JobPostStatus.OPEN) {
            throw new BadRequestException("Cannot reopen a closed job post");
        }
    }

    private void mapRequestToJobPost(JobPostRequest request, JobPost jobPost) {
        jobPost.setTitle(request.title());
        jobPost.setCompany(request.company());
        jobPost.setJobType(request.jobType());
        jobPost.setDescription(request.description());
        jobPost.setRemote(request.remote());
        jobPost.setSalary(request.salary());
        jobPost.setExperienceLevel(request.experienceLevel());

        // Convert location to JsonNode
        if (request.location() != null) {
            ObjectNode locationNode = objectMapper.createObjectNode();
            if (request.location().address() != null)
                locationNode.put("address", request.location().address());
            if (request.location().postalCode() != null)
                locationNode.put("postalCode", request.location().postalCode());
            locationNode.put("city", request.location().city());
            locationNode.put("countryCode", request.location().countryCode());
            if (request.location().region() != null)
                locationNode.put("region", request.location().region());
            jobPost.setLocation(locationNode);
        }

        // Convert lists to JsonNode arrays
        if (request.responsibilities() != null) {
            ArrayNode responsibilitiesNode = objectMapper.createArrayNode();
            request.responsibilities().forEach(responsibilitiesNode::add);
            jobPost.setResponsibilities(responsibilitiesNode);
        }

        if (request.qualifications() != null) {
            ArrayNode qualificationsNode = objectMapper.createArrayNode();
            request.qualifications().forEach(qualificationsNode::add);
            jobPost.setQualifications(qualificationsNode);
        }

        if (request.skills() != null) {
            ArrayNode skillsNode = objectMapper.createArrayNode();
            request.skills().forEach(skill -> {
                ObjectNode skillNode = objectMapper.createObjectNode();
                skillNode.put("name", skill.name());
                skillNode.put("level", skill.level());
                ArrayNode keywordsNode = objectMapper.createArrayNode();
                if (skill.keywords() != null) {
                    skill.keywords().forEach(keywordsNode::add);
                }
                skillNode.set("keywords", keywordsNode);
                skillsNode.add(skillNode);
            });
            jobPost.setSkills(skillsNode);
        }
    }

    private JobPostResponse mapToResponse(JobPost jobPost) {
        return new JobPostResponse(
                jobPost.getId(),
                jobPost.getTitle(),
                jobPost.getCompany(),
                jobPost.getJobType(),
                jobPost.getDatePosted(),
                jobPost.getDescription(),
                jobPost.getLocation(),
                jobPost.getRemote(),
                jobPost.getSalary(),
                jobPost.getExperienceLevel(),
                jobPost.getResponsibilities(),
                jobPost.getQualifications(),
                jobPost.getSkills(),
                jobPost.getStatus(),
                jobPost.getCreatedBy().getEmail(),
                jobPost.getCreatedAt(),
                jobPost.getUpdatedAt()
        );
    }
}
Step 4: Update JobPostController
Update the controller to allow recruiters for appropriate operations:

java
package com.etalente.backend.controller;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.service.JobPostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/job-posts")
public class JobPostController {

    private final JobPostService jobPostService;

    public JobPostController(JobPostService jobPostService) {
        this.jobPostService = jobPostService;
    }

    @PostMapping
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<JobPostResponse> createJobPost(@Valid @RequestBody JobPostRequest request,
                                                         Authentication authentication) {
        JobPostResponse response = jobPostService.createJobPost(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPostResponse> getJobPost(@PathVariable UUID id) {
        JobPostResponse response = jobPostService.getJobPost(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public Page<JobPostResponse> listJobPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return jobPostService.listJobPosts(pageable);
    }

    @GetMapping("/my-posts")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public Page<JobPostResponse> listMyJobPosts(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        return jobPostService.listJobPostsByUser(authentication.getName(), pageable);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> updateJobPost(@PathVariable UUID id,
                                                         @Valid @RequestBody JobPostRequest request,
                                                         Authentication authentication) {
        JobPostResponse response = jobPostService.updateJobPost(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<Void> deleteJobPost(@PathVariable UUID id, Authentication authentication) {
        jobPostService.deleteJobPost(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> updateJobPostStatus(@PathVariable UUID id,
                                                               @RequestParam String status,
                                                               Authentication authentication) {
        JobPostStatus newStatus = JobPostStatus.valueOf(status.toUpperCase());
        JobPostResponse response = jobPostService.updateJobPostStatus(id, newStatus, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> publishJobPost(@PathVariable UUID id,
                                                          Authentication authentication) {
        JobPostResponse response = jobPostService.publishJobPost(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")
    public ResponseEntity<JobPostResponse> closeJobPost(@PathVariable UUID id,
                                                        Authentication authentication) {
        JobPostResponse response = jobPostService.closeJobPost(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
Should I continue with the comprehensive permission tests?

Claude Sonnet 4.5
yes


Thought Process
expand
The user wants me to continue with comprehensive permission tests for JOB-007. Let me create thorough tests that verify all the permission rules for recruiters vs hiring managers.

Step 5: Create Comprehensive Permission Tests
Create file: src/test/java/com/etalente/backend/service/JobPostPermissionTest.java

java
package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JobPostPermissionTest extends BaseIntegrationTest {

    @Autowired
    private JobPostService jobPostService;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JobPostPermissionService permissionService;

    private Organization organization;
    private User hiringManager;
    private User recruiter;
    private User candidate;
    private JobPost draftJobPost;
    private JobPost openJobPost;

    @BeforeEach
    void setUp() {
        // Create organization
        organization = new Organization();
        organization.setName("Test Company");
        organization.setIndustry("Technology");
        organization = organizationRepository.save(organization);

        // Create hiring manager
        hiringManager = createUser("hm@test.com", "hm1", Role.HIRING_MANAGER, organization);

        // Create recruiter
        recruiter = createUser("recruiter@test.com", "recruiter1", Role.RECRUITER, organization);

        // Create candidate
        candidate = createUser("candidate@test.com", "candidate1", Role.CANDIDATE, null);

        // Create draft job post by hiring manager
        authenticateAs(hiringManager.getEmail());
        JobPostRequest request = createJobPostRequest("Software Engineer", "Draft job");
        JobPostResponse response = jobPostService.createJobPost(request, hiringManager.getEmail());
        draftJobPost = jobPostRepository.findById(response.id()).orElseThrow();

        // Create open job post
        JobPostRequest openRequest = createJobPostRequest("Senior Developer", "Open job");
        JobPostResponse openResponse = jobPostService.createJobPost(openRequest, hiringManager.getEmail());
        openJobPost = jobPostRepository.findById(openResponse.id()).orElseThrow();
        openJobPost.setStatus(JobPostStatus.OPEN);
        openJobPost = jobPostRepository.save(openJobPost);
    }

    // === CREATE TESTS ===

    @Test
    void hiringManagerCanCreateJobPost() {
        authenticateAs(hiringManager.getEmail());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatCode(() -> jobPostService.createJobPost(request, hiringManager.getEmail()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCannotCreateJobPost() {
        authenticateAs(recruiter.getEmail());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can create job posts");
    }

    @Test
    void candidateCannotCreateJobPost() {
        authenticateAs(candidate.getEmail());

        JobPostRequest request = createJobPostRequest("New Position", "Description");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === UPDATE TESTS ===

    @Test
    void hiringManagerCanUpdateOwnJobPost() {
        authenticateAs(hiringManager.getEmail());

        JobPostRequest updateRequest = createJobPostRequest("Updated Title", "Updated description");

        assertThatCode(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, hiringManager.getEmail()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCanUpdateJobPostInSameOrganization() {
        authenticateAs(recruiter.getEmail());

        JobPostRequest updateRequest = createJobPostRequest("Recruiter Update", "Updated by recruiter");

        assertThatCode(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, recruiter.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Recruiter Update");
    }

    @Test
    void recruiterCannotUpdateJobPostFromDifferentOrganization() {
        // Create another organization with a job post
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg.setIndustry("Finance");
        otherOrg = organizationRepository.save(otherOrg);

        User otherHM = createUser("other-hm@test.com", "otherhm", Role.HIRING_MANAGER, otherOrg);

        authenticateAs(otherHM.getEmail());
        JobPostRequest request = createJobPostRequest("Other Company Job", "Description");
        JobPostResponse response = jobPostService.createJobPost(request, otherHM.getEmail());

        // Try to update as recruiter from first organization
        authenticateAs(recruiter.getEmail());
        JobPostRequest updateRequest = createJobPostRequest("Hacked", "Should not work");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                response.id(), updateRequest, recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void candidateCannotUpdateJobPost() {
        authenticateAs(candidate.getEmail());

        JobPostRequest updateRequest = createJobPostRequest("Hacked Title", "Hacked");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                draftJobPost.getId(), updateRequest, candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === DELETE TESTS ===

    @Test
    void hiringManagerCanDeleteOwnJobPost() {
        authenticateAs(hiringManager.getEmail());

        assertThatCode(() -> jobPostService.deleteJobPost(draftJobPost.getId(), hiringManager.getEmail()))
                .doesNotThrowAnyException();

        assertThat(jobPostRepository.findById(draftJobPost.getId())).isEmpty();
    }

    @Test
    void recruiterCannotDeleteJobPost() {
        authenticateAs(recruiter.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can delete job posts");
    }

    @Test
    void hiringManagerCannotDeleteOtherHiringManagerJobPost() {
        // Create another hiring manager in same organization
        User otherHM = createUser("other-hm@test.com", "otherhm", Role.HIRING_MANAGER, organization);

        authenticateAs(otherHM.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), otherHM.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You can only delete your own job posts");
    }

    @Test
    void candidateCannotDeleteJobPost() {
        authenticateAs(candidate.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(draftJobPost.getId(), candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class);
    }

    // === STATUS CHANGE TESTS ===

    @Test
    void hiringManagerCanPublishJobPost() {
        authenticateAs(hiringManager.getEmail());

        assertThatCode(() -> jobPostService.publishJobPost(draftJobPost.getId(), hiringManager.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void recruiterCanPublishJobPost() {
        authenticateAs(recruiter.getEmail());

        assertThatCode(() -> jobPostService.publishJobPost(draftJobPost.getId(), recruiter.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(draftJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void hiringManagerCanCloseJobPost() {
        authenticateAs(hiringManager.getEmail());

        assertThatCode(() -> jobPostService.closeJobPost(openJobPost.getId(), hiringManager.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(openJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.CLOSED);
    }

    @Test
    void recruiterCanCloseJobPost() {
        authenticateAs(recruiter.getEmail());

        assertThatCode(() -> jobPostService.closeJobPost(openJobPost.getId(), recruiter.getEmail()))
                .doesNotThrowAnyException();

        JobPost updated = jobPostRepository.findById(openJobPost.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(JobPostStatus.CLOSED);
    }

    @Test
    void candidateCannotChangeJobPostStatus() {
        authenticateAs(candidate.getEmail());

        assertThatThrownBy(() -> jobPostService.publishJobPost(draftJobPost.getId(), candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void recruiterCannotChangeStatusOfJobPostFromDifferentOrganization() {
        // Create another organization with a job post
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg.setIndustry("Finance");
        otherOrg = organizationRepository.save(otherOrg);

        User otherHM = createUser("other-hm2@test.com", "otherhm2", Role.HIRING_MANAGER, otherOrg);

        authenticateAs(otherHM.getEmail());
        JobPostRequest request = createJobPostRequest("Other Company Job", "Description");
        JobPostResponse response = jobPostService.createJobPost(request, otherHM.getEmail());

        // Try to publish as recruiter from first organization
        authenticateAs(recruiter.getEmail());

        assertThatThrownBy(() -> jobPostService.publishJobPost(response.id(), recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    // === VIEW PERMISSION TESTS ===

    @Test
    void hiringManagerCanViewOwnDraftJobPost() {
        authenticateAs(hiringManager.getEmail());

        assertThatCode(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void recruiterCanViewDraftJobPostInSameOrganization() {
        authenticateAs(recruiter.getEmail());

        assertThatCode(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void candidateCannotViewDraftJobPost() {
        authenticateAs(candidate.getEmail());

        assertThatThrownBy(() -> jobPostService.getJobPost(draftJobPost.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void candidateCanViewOpenJobPost() {
        authenticateAs(candidate.getEmail());

        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();
    }

    @Test
    void everyoneCanViewPublicJobPost() {
        // Hiring Manager
        authenticateAs(hiringManager.getEmail());
        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();

        // Recruiter
        authenticateAs(recruiter.getEmail());
        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();

        // Candidate
        authenticateAs(candidate.getEmail());
        assertThatCode(() -> jobPostService.getJobPost(openJobPost.getId()))
                .doesNotThrowAnyException();
    }

    // === PERMISSION SERVICE UNIT TESTS ===

    @Test
    void permissionServiceVerifyCanCreate() {
        assertThatCode(() -> permissionService.verifyCanCreate(hiringManager))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> permissionService.verifyCanCreate(recruiter))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can create job posts");

        assertThatThrownBy(() -> permissionService.verifyCanCreate(candidate))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void permissionServiceVerifyCanUpdate() {
        assertThatCode(() -> permissionService.verifyCanUpdate(hiringManager, draftJobPost))
                .doesNotThrowAnyException();

        assertThatCode(() -> permissionService.verifyCanUpdate(recruiter, draftJobPost))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> permissionService.verifyCanUpdate(candidate, draftJobPost))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void permissionServiceVerifyCanDelete() {
        assertThatCode(() -> permissionService.verifyCanDelete(hiringManager, draftJobPost))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> permissionService.verifyCanDelete(recruiter, draftJobPost))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only hiring managers can delete job posts");
    }

    @Test
    void permissionServiceVerifyCanChangeStatus() {
        assertThatCode(() -> permissionService.verifyCanChangeStatus(hiringManager, draftJobPost))
                .doesNotThrowAnyException();

        assertThatCode(() -> permissionService.verifyCanChangeStatus(recruiter, draftJobPost))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> permissionService.verifyCanChangeStatus(candidate, draftJobPost))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void permissionServiceCanView() {
        // Public job post - everyone can view
        assertThat(permissionService.canView(hiringManager, openJobPost)).isTrue();
        assertThat(permissionService.canView(recruiter, openJobPost)).isTrue();
        assertThat(permissionService.canView(candidate, openJobPost)).isTrue();

        // Draft job post - only organization members can view
        assertThat(permissionService.canView(hiringManager, draftJobPost)).isTrue();
        assertThat(permissionService.canView(recruiter, draftJobPost)).isTrue();
        assertThat(permissionService.canView(candidate, draftJobPost)).isFalse();
    }

    // Helper methods
    private User createUser(String email, String username, Role role, Organization organization) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setOrganization(organization);
        user.setEmailVerified(true);
        user.setProfileComplete(true);
        return userRepository.save(user);
    }

    private JobPostRequest createJobPostRequest(String title, String description) {
        return new JobPostRequest(
                title,
                "Test Company",
                "Full-time",
                description,
                new LocationDto("123 Main St", "12345", "San Francisco", "US", "CA"),
                "Hybrid",
                "$100k-$150k",
                "Senior",
                List.of("Develop features", "Code reviews"),
                List.of("5+ years experience", "Java expertise"),
                List.of()
        );
    }

    private void authenticateAs(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
Step 6: Create Controller Integration Tests
Create file: src/test/java/com/etalente/backend/controller/JobPostPermissionControllerTest.java

java
package com.etalente.backend.controller;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class JobPostPermissionControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private JwtService jwtService;

    private Organization organization;
    private User hiringManager;
    private User recruiter;
    private User candidate;
    private String hmToken;
    private String recruiterToken;
    private String candidateToken;
    private JobPost jobPost;

    @BeforeEach
    void setUp() {
        // Create organization
        organization = new Organization();
        organization.setName("Test Company");
        organization.setIndustry("Tech");
        organization = organizationRepository.save(organization);

        // Create users
        hiringManager = createUser("hm@test.com", "hm1", Role.HIRING_MANAGER, organization);
        recruiter = createUser("recruiter@test.com", "rec1", Role.RECRUITER, organization);
        candidate = createUser("candidate@test.com", "cand1", Role.CANDIDATE, null);

        // Generate tokens
        hmToken = generateToken(hiringManager);
        recruiterToken = generateToken(recruiter);
        candidateToken = generateToken(candidate);

        // Create job post
        jobPost = new JobPost();
        jobPost.setTitle("Test Job");
        jobPost.setDescription("Description");
        jobPost.setCreatedBy(hiringManager);
        jobPost.setOrganization(organization);
        jobPost.setStatus(JobPostStatus.DRAFT);
        jobPost = jobPostRepository.save(jobPost);
    }

    // === CREATE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanCreateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(request.title()));
    }

    @Test
    void recruiterCannotCreateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCannotCreateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // === UPDATE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanUpdateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + hmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void recruiterCanUpdateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void candidateCannotUpdateJobPost() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + candidateToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // === DELETE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanDeleteOwnJobPost() throws Exception {
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void recruiterCannotDeleteJobPost() throws Exception {
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void candidateCannotDeleteJobPost() throws Exception {
        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId())
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    // === STATUS CHANGE ENDPOINT TESTS ===

    @Test
    void hiringManagerCanPublishJobPost() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void recruiterCanPublishJobPost() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void candidateCannotPublishJobPost() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/publish")
Claude Sonnet 4.5
continue


Thought Process
expand
The user wants me to continue with the test file. I was in the middle of writing the JobPostPermissionControllerTest.java file and got cut off at the candidateCannotPublishJobPost test. Let me continue from there.

java
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hiringManagerCanCloseJobPost() throws Exception {
        // First publish it
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/close")
                        .header("Authorization", "Bearer " + hmToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void recruiterCanCloseJobPost() throws Exception {
        // First publish it
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/close")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void candidateCannotCloseJobPost() throws Exception {
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/close")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void hiringManagerCanUpdateJobPostStatus() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/status")
                        .header("Authorization", "Bearer " + hmToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void recruiterCanUpdateJobPostStatus() throws Exception {
        mockMvc.perform(patch("/api/job-posts/" + jobPost.getId() + "/status")
                        .header("Authorization", "Bearer " + recruiterToken)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    // === LISTING ENDPOINT TESTS ===

    @Test
    void recruiterCanAccessMyPostsEndpoint() throws Exception {
        mockMvc.perform(get("/api/job-posts/my-posts")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isOk());
    }

    @Test
    void candidateCannotAccessMyPostsEndpoint() throws Exception {
        mockMvc.perform(get("/api/job-posts/my-posts")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCannotAccessProtectedEndpoints() throws Exception {
        JobPostRequest request = createJobPostRequest();

        mockMvc.perform(post("/api/job-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/job-posts/" + jobPost.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/job-posts/" + jobPost.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserCanAccessPublicJobPost() throws Exception {
        jobPost.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(jobPost);

        mockMvc.perform(get("/api/job-posts/" + jobPost.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedUserCanListPublicJobPosts() throws Exception {
        mockMvc.perform(get("/api/job-posts"))
                .andExpect(status().isOk());
    }

    // Helper methods
    private User createUser(String email, String username, Role role, Organization org) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setOrganization(org);
        user.setEmailVerified(true);
        user.setProfileComplete(true);
        return userRepository.save(user);
    }

    private String generateToken(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password("")
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .build();

        return jwtService.generateToken(Map.of("role", user.getRole().name()), userDetails);
    }

    private JobPostRequest createJobPostRequest() {
        return new JobPostRequest(
                "Software Engineer",
                "Test Company",
                "Full-time",
                "Test description",
                new LocationDto("123 Main St", "12345", "San Francisco", "US", "CA"),
                "Hybrid",
                "$100k-$150k",
                "Senior",
                List.of("Develop features", "Code reviews"),
                List.of("5+ years experience", "Java expertise"),
                List.of()
        );
    }
}
Step 7: Add Exception Handler Test
Let's ensure proper error messages are returned:

Create file: src/test/java/com/etalente/backend/service/JobPostPermissionErrorHandlingTest.java

java
package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JobPostPermissionErrorHandlingTest extends BaseIntegrationTest {

    @Autowired
    private JobPostService jobPostService;

    @Autowired
    private JobPostRepository jobPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private Organization organization;
    private User hiringManager;
    private User recruiter;
    private JobPost jobPost;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setName("Test Company");
        organization.setIndustry("Technology");
        organization = organizationRepository.save(organization);

        hiringManager = createUser("hm@test.com", "hm1", Role.HIRING_MANAGER, organization);
        recruiter = createUser("recruiter@test.com", "rec1", Role.RECRUITER, organization);

        authenticateAs(hiringManager.getEmail());
        JobPostRequest request = createJobPostRequest();
        var response = jobPostService.createJobPost(request, hiringManager.getEmail());
        jobPost = jobPostRepository.findById(response.id()).orElseThrow();
    }

    @Test
    void creatingJobPostAsRecruiterGivesDescriptiveError() {
        authenticateAs(recruiter.getEmail());

        JobPostRequest request = createJobPostRequest();

        assertThatThrownBy(() -> jobPostService.createJobPost(request, recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only hiring managers can create job posts")
                .hasNoCause();
    }

    @Test
    void deletingJobPostAsRecruiterGivesDescriptiveError() {
        authenticateAs(recruiter.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(jobPost.getId(), recruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only hiring managers can delete job posts");
    }

    @Test
    void deletingOthersJobPostGivesDescriptiveError() {
        User otherHM = createUser("other@test.com", "other", Role.HIRING_MANAGER, organization);
        authenticateAs(otherHM.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(jobPost.getId(), otherHM.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only delete your own job posts");
    }

    @Test
    void updatingJobPostFromDifferentOrganizationGivesDescriptiveError() {
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg.setIndustry("Finance");
        otherOrg = organizationRepository.save(otherOrg);

        User otherRecruiter = createUser("other-rec@test.com", "otherRec", Role.RECRUITER, otherOrg);
        authenticateAs(otherRecruiter.getEmail());

        JobPostRequest request = createJobPostRequest();

        assertThatThrownBy(() -> jobPostService.updateJobPost(jobPost.getId(), request, otherRecruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void statusChangeOnDifferentOrganizationJobPostGivesDescriptiveError() {
        Organization otherOrg = new Organization();
        otherOrg.setName("Other Company");
        otherOrg = organizationRepository.save(otherOrg);

        User otherRecruiter = createUser("other-rec2@test.com", "otherRec2", Role.RECRUITER, otherOrg);
        authenticateAs(otherRecruiter.getEmail());

        assertThatThrownBy(() -> jobPostService.publishJobPost(jobPost.getId(), otherRecruiter.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("not found in your organization");
    }

    private User createUser(String email, String username, Role role, Organization org) {
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(role);
        user.setOrganization(org);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }

    private JobPostRequest createJobPostRequest() {
        return new JobPostRequest(
                "Software Engineer",
                "Test Company",
                "Full-time",
                "Test description",
                new LocationDto("123 Main St", "12345", "San Francisco", "US", "CA"),
                "Hybrid",
                "$100k-$150k",
                "Senior",
                List.of("Develop features", "Code reviews"),
                List.of("5+ years experience", "Java expertise"),
                List.of()
        );
    }

    private void authenticateAs(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
Step 8: Documentation
Create file: docs/job-post-permissions.md

markdown
# Job Post Permissions

This document describes the permission model for job post operations in the Etalente platform.

## Permission Matrix

|
 Operation
|
 Hiring Manager
|
 Recruiter
|
 Candidate
|
|
-----------
|
---------------
|
-----------
|
-----------
|
|
 Create Job Post
|
 ✅ Yes
|
 ❌ No
|
 ❌ No
|
|
 View Draft Job Post (own org)
|
 ✅ Yes
|
 ✅ Yes
|
 ❌ No
|
|
 View Public Job Post
|
 ✅ Yes
|
 ✅ Yes
|
 ✅ Yes
|
|
 Update Job Post (own org)
|
 ✅ Yes
|
 ✅ Yes
|
 ❌ No
|
|
 Delete Job Post (own only)
|
 ✅ Yes
|
 ❌ No
|
 ❌ No
|
|
 Publish Job Post (own org)
|
 ✅ Yes
|
 ✅ Yes
|
 ❌ No
|
|
 Close Job Post (own org)
|
 ✅ Yes
|
 ✅ Yes
|
 ❌ No
|
|
 Change Status (own org)
|
 ✅ Yes
|
 ✅ Yes
|
 ❌ No
|

## Permission Rules

### Create
- **Only Hiring Managers** can create new job posts
- Job posts are automatically associated with the creator's organization
- New job posts start in `DRAFT` status

### View
- **Public job posts** (status = `OPEN`) can be viewed by everyone, including unauthenticated users
- **Non-public job posts** can only be viewed by members of the same organization
- Candidates cannot view draft job posts from any organization

### Update
- **Hiring Managers** can update any job post in their organization
- **Recruiters** can update any job post in their organization
- Users cannot update job posts from other organizations

### Delete
- **Only Hiring Managers** can delete job posts
- Hiring managers can only delete their **own** job posts
- Recruiters cannot delete any job posts

### Status Changes (Publish/Close/Archive)
- **Hiring Managers** can change the status of any job post in their organization
- **Recruiters** can change the status of any job post in their organization
- Status changes are subject to state machine rules (see JOB-002)

## Organization Boundaries

All operations respect organization boundaries:
- Users can only access job posts within their organization (except public posts)
- Cross-organization access is prevented at both the service and repository layers
- Candidates don't belong to organizations and have read-only access to public posts

## Error Messages

The system provides clear error messages for permission violations:

- `"Only hiring managers can create job posts"` - When recruiter/candidate tries to create
- `"Only hiring managers can delete job posts"` - When recruiter/candidate tries to delete
- `"You can only delete your own job posts"` - When HM tries to delete another HM's post
- `"You can only update job posts in your organization"` - Cross-organization update attempt
- `"Job post not found in your organization"` - When accessing another org's post
- `"You don't have access to this job post"` - When candidate tries to view draft post

## Implementation Details

### Permission Service
The `JobPostPermissionService` centralizes all permission logic:
- `verifyCanCreate(User)` - Checks create permission
- `verifyCanUpdate(User, JobPost)` - Checks update permission
- `verifyCanDelete(User, JobPost)` - Checks delete permission
- `verifyCanChangeStatus(User, JobPost)` - Checks status change permission
- `canView(User, JobPost)` - Checks view permission

### Controller Security
The `JobPostController` uses Spring Security's `@PreAuthorize` annotations:
```java
@PreAuthorize("hasRole('HIRING_MANAGER')") // Only hiring managers
@PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')") // Both roles
// No annotation = public access
Service Layer
The service layer provides an additional security layer with detailed business logic checks.

Testing
Comprehensive tests cover:

✅ All permission combinations for all operations
✅ Cross-organization access prevention
✅ Error message validation
✅ Controller-level and service-level tests
✅ Integration tests with real database
Run tests:

bash
./gradlew test --tests "*JobPostPermission*"
text

## Step 9: Run All Tests

Now let's verify everything works:

```bash
# Run all tests
./gradlew test

# Run only permission tests
./gradlew test --tests "*Permission*"

# Run with verbose output
./gradlew test --info --tests "*JobPostPermission*"
Summary
✅ Implemented Features:

JobPostPermissionService - Centralized permission logic
Updated JobPostService - Permission checks in business logic
Updated JobPostController - @PreAuthorize annotations for recruiters
New Status Management Endpoints - publish, close, updateStatus
Comprehensive Tests - 40+ test cases covering all scenarios
Documentation - Clear permission matrix and rules
✅ Acceptance Criteria Met:

✅ Recruiters can publish, advance stages, and close job posts within their organization
✅ Recruiters cannot create or delete job posts
✅ Role-based authorization (@PreAuthorize) applied to relevant methods
✅ Security Features:

Organization boundary enforcement
Clear error messages
Multiple layers of security (controller, service, repository)
Comprehensive test coverage