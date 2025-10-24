I can see the security gap:

Critical Issue: JobPost entity doesn't have an organization field, which means:

Hiring managers from different organizations can see each other's job posts
Organization boundaries are not enforced
Security relies only on checking createdBy, which is insufficient
Implementation Plan for ORG-002
Let's implement this systematically:

Step 1: Database Migration
First, we need to add the organization relationship to job_posts table.

Create file: src/main/resources/db/migration/V8__Add_organization_to_job_posts.sql

sql
-- Add organization_id column to job_posts
ALTER TABLE job_posts ADD COLUMN organization_id UUID;

-- Add foreign key constraint
ALTER TABLE job_posts ADD CONSTRAINT fk_job_posts_organization
    FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- Set organization_id based on the creator's organization
UPDATE job_posts jp
SET organization_id = u.organization_id
FROM users u
WHERE jp.created_by_id = u.id AND u.organization_id IS NOT NULL;

-- Create index for better query performance
CREATE INDEX idx_job_posts_organization ON job_posts(organization_id);

-- Add constraint: job posts must have an organization (after data migration)
-- We'll add this in a comment for now since existing posts might not have it
-- ALTER TABLE job_posts ALTER COLUMN organization_id SET NOT NULL;
Step 2: Update JobPost Entity
Update your JobPost.java to add the organization relationship:

java
@Entity
@Table(name = "job_posts")
@EntityListeners(AuditingEntityListener.class)
public class JobPost {

    // ... existing fields ...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    // ... existing fields ...

    // Add getter and setter
    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }
}
Should I continue with the remaining steps?

Claude Sonnet 4.5
yes


Thought Process
expand
The user wants me to continue with the remaining steps for implementing multi-tenancy data isolation. Let me proceed with the implementation step-by-step.

Step 3: Create Organization Context Utility
This utility will extract the current user's organization from the security context.

Create file: src/main/java/com/etalente/backend/security/OrganizationContext.java

java
package com.etalente.backend.security;

import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrganizationContext {

    private final UserRepository userRepository;

    public OrganizationContext(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get the organization ID of the currently authenticated user
     * @return Organization ID or null if user doesn't belong to an organization
     */
    public UUID getCurrentOrganizationId() {
        User user = getCurrentUser();
        if (user.getOrganization() == null) {
            return null;
        }
        return user.getOrganization().getId();
    }

    /**
     * Get the organization of the currently authenticated user
     * @return Organization or null if user doesn't belong to one
     */
    public Organization getCurrentOrganization() {
        User user = getCurrentUser();
        return user.getOrganization();
    }

    /**
     * Get the organization ID and throw exception if user doesn't belong to one
     * @return Organization ID
     * @throws UnauthorizedException if user doesn't have an organization
     */
    public UUID requireOrganizationId() {
        UUID orgId = getCurrentOrganizationId();
        if (orgId == null) {
            throw new UnauthorizedException("User must belong to an organization to perform this action");
        }
        return orgId;
    }

    /**
     * Get the organization and throw exception if user doesn't belong to one
     * @return Organization
     * @throws UnauthorizedException if user doesn't have an organization
     */
    public Organization requireOrganization() {
        Organization org = getCurrentOrganization();
        if (org == null) {
            throw new UnauthorizedException("User must belong to an organization to perform this action");
        }
        return org;
    }

    /**
     * Check if the current user belongs to the specified organization
     * @param organizationId Organization ID to check
     * @return true if user belongs to the organization
     */
    public boolean belongsToOrganization(UUID organizationId) {
        if (organizationId == null) {
            return false;
        }
        UUID currentOrgId = getCurrentOrganizationId();
        return organizationId.equals(currentOrgId);
    }

    /**
     * Verify that the current user belongs to the specified organization
     * @param organizationId Organization ID to verify
     * @throws UnauthorizedException if user doesn't belong to the organization
     */
    public void verifyOrganizationAccess(UUID organizationId) {
        if (!belongsToOrganization(organizationId)) {
            throw new UnauthorizedException("You don't have access to this organization's data");
        }
    }

    /**
     * Check if the current user is a candidate (candidates don't have organizations)
     * @return true if user is a candidate
     */
    public boolean isCandidate() {
        User user = getCurrentUser();
        return user.getRole() == Role.CANDIDATE;
    }

    /**
     * Get the current authenticated user
     * @return User entity
     * @throws UnauthorizedException if no user is authenticated
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new UnauthorizedException("Invalid authentication principal");
        }

        String email = ((UserDetails) principal).getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    /**
     * Get the current user's email
     * @return User email
     */
    public String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }
}
Step 4: Update JobPostRepository
Add organization-aware query methods:

java
package com.etalente.backend.repository;

import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobPostRepository extends JpaRepository<JobPost, UUID> {

    // Organization-filtered queries
    Page<JobPost> findByOrganization(Organization organization, Pageable pageable);

    Page<JobPost> findByOrganizationAndStatus(Organization organization, JobPostStatus status, Pageable pageable);

    Optional<JobPost> findByIdAndOrganization(UUID id, Organization organization);

    // User-specific queries (within organization)
    @Query("SELECT jp FROM JobPost jp WHERE jp.createdBy.email = :email AND jp.organization = :organization")
    Page<JobPost> findByCreatedByEmailAndOrganization(
            @Param("email") String email,
            @Param("organization") Organization organization,
            Pageable pageable);

    // Public queries (no organization filter needed)
    @Query("SELECT jp FROM JobPost jp WHERE jp.status = 'OPEN' ORDER BY jp.createdAt DESC")
    Page<JobPost> findPublishedJobs(Pageable pageable);

    @Query("SELECT jp FROM JobPost jp WHERE " +
            "(LOWER(jp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(jp.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(jp.company) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "jp.status = 'OPEN'")
    Page<JobPost> searchJobPosts(@Param("keyword") String keyword, Pageable pageable);

    // Keep legacy methods for backward compatibility (mark as deprecated)
    @Deprecated
    Page<JobPost> findByCreatedByEmail(String email, Pageable pageable);

    @Deprecated
    Page<JobPost> findByStatus(JobPostStatus status, Pageable pageable);
}
Step 5: Update JobPostServiceImpl
Update the service to enforce organization boundaries:

java
package com.etalente.backend.service.impl;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.JobPost;
import com.etalente.backend.model.JobPostStatus;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.OrganizationContext;
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

    public JobPostServiceImpl(JobPostRepository jobPostRepository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper,
                              OrganizationContext organizationContext) {
        this.jobPostRepository = jobPostRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.organizationContext = organizationContext;
    }

    @Override
    public JobPostResponse createJobPost(JobPostRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

        // If the job is not public, verify organization access
        if (jobPost.getStatus() != JobPostStatus.OPEN) {
            if (!organizationContext.isCandidate()) {
                // For hiring managers and recruiters, check organization access
                organizationContext.verifyOrganizationAccess(jobPost.getOrganization().getId());
            } else {
                // Candidates can't see non-public job posts
                throw new UnauthorizedException("You don't have access to this job post");
            }
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
        Organization organization = organizationContext.requireOrganization();

        JobPost jobPost = jobPostRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found in your organization"));

        if (!jobPost.getCreatedBy().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You can only update your own job posts");
        }

        mapRequestToJobPost(request, jobPost);
        JobPost updated = jobPostRepository.save(jobPost);
        return mapToResponse(updated);
    }

    @Override
    public void deleteJobPost(UUID id, String userEmail) {
        Organization organization = organizationContext.requireOrganization();

        JobPost jobPost = jobPostRepository.findByIdAndOrganization(id, organization)
                .orElseThrow(() -> new ResourceNotFoundException("Job post not found in your organization"));

        if (!jobPost.getCreatedBy().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You can only delete your own job posts");
        }

        jobPostRepository.delete(jobPost);
    }

    // ... existing mapRequestToJobPost and mapToResponse methods remain the same ...

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
Step 6: Create Integration Tests
Create file: src/test/java/com/etalente/backend/security/OrganizationContextTest.java

java
package com.etalente.backend.security;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrganizationContextTest extends BaseIntegrationTest {

    @Autowired
    private OrganizationContext organizationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private Organization org1;
    private Organization org2;
    private User hiringManager1;
    private User hiringManager2;
    private User candidate;

    @BeforeEach
    void setUp() {
        // Create organizations
        org1 = new Organization();
        org1.setName("Company A");
        org1.setIndustry("Tech");
        org1 = organizationRepository.save(org1);

        org2 = new Organization();
        org2.setName("Company B");
        org2.setIndustry("Finance");
        org2 = organizationRepository.save(org2);

        // Create hiring manager for org1
        hiringManager1 = new User();
        hiringManager1.setEmail("hm1@companya.com");
        hiringManager1.setUsername("hm1");
        hiringManager1.setRole(Role.HIRING_MANAGER);
        hiringManager1.setOrganization(org1);
        hiringManager1.setEmailVerified(true);
        hiringManager1 = userRepository.save(hiringManager1);

        // Create hiring manager for org2
        hiringManager2 = new User();
        hiringManager2.setEmail("hm2@companyb.com");
        hiringManager2.setUsername("hm2");
        hiringManager2.setRole(Role.HIRING_MANAGER);
        hiringManager2.setOrganization(org2);
        hiringManager2.setEmailVerified(true);
        hiringManager2 = userRepository.save(hiringManager2);

        // Create candidate (no organization)
        candidate = new User();
        candidate.setEmail("candidate@example.com");
        candidate.setUsername("candidate1");
        candidate.setRole(Role.CANDIDATE);
        candidate.setEmailVerified(true);
        candidate = userRepository.save(candidate);
    }

    @Test
    void shouldGetCurrentOrganizationForHiringManager() {
        authenticateAs(hiringManager1.getEmail());

        UUID orgId = organizationContext.getCurrentOrganizationId();
        assertThat(orgId).isEqualTo(org1.getId());

        Organization org = organizationContext.getCurrentOrganization();
        assertThat(org.getName()).isEqualTo("Company A");
    }

    @Test
    void shouldReturnNullOrganizationForCandidate() {
        authenticateAs(candidate.getEmail());

        UUID orgId = organizationContext.getCurrentOrganizationId();
        assertThat(orgId).isNull();

        Organization org = organizationContext.getCurrentOrganization();
        assertThat(org).isNull();
    }

    @Test
    void shouldThrowExceptionWhenRequireOrganizationForCandidate() {
        authenticateAs(candidate.getEmail());

        assertThatThrownBy(() -> organizationContext.requireOrganizationId())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("must belong to an organization");
    }

    @Test
    void shouldVerifyOrganizationAccess() {
        authenticateAs(hiringManager1.getEmail());

        // Should not throw for same organization
        assertThatCode(() -> organizationContext.verifyOrganizationAccess(org1.getId()))
                .doesNotThrowAnyException();

        // Should throw for different organization
        assertThatThrownBy(() -> organizationContext.verifyOrganizationAccess(org2.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void shouldCheckBelongsToOrganization() {
        authenticateAs(hiringManager1.getEmail());

        assertThat(organizationContext.belongsToOrganization(org1.getId())).isTrue();
        assertThat(organizationContext.belongsToOrganization(org2.getId())).isFalse();
        assertThat(organizationContext.belongsToOrganization(null)).isFalse();
    }

    @Test
    void shouldIdentifyCandidate() {
        authenticateAs(candidate.getEmail());
        assertThat(organizationContext.isCandidate()).isTrue();

        authenticateAs(hiringManager1.getEmail());
        assertThat(organizationContext.isCandidate()).isFalse();
    }

    private void authenticateAs(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
Should I continue with the job post multi-tenancy tests?

Claude Sonnet 4.5
yes


Thought Process
expand
The user wants me to continue with the job post multi-tenancy tests. I'll create comprehensive integration tests to verify that organization boundaries are properly enforced for job posts.

Step 7: Create Job Post Multi-Tenancy Integration Tests
Create file: src/test/java/com/etalente/backend/service/JobPostMultiTenancyTest.java

java
package com.etalente.backend.service;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JobPostMultiTenancyTest extends BaseIntegrationTest {

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

    private Organization orgA;
    private Organization orgB;
    private User hiringManagerA;
    private User hiringManagerB;
    private User recruiterA;
    private User candidate;
    private JobPost jobPostOrgA;
    private JobPost jobPostOrgB;

    @BeforeEach
    void setUp() {
        // Create Organization A
        orgA = new Organization();
        orgA.setName("Company A");
        orgA.setIndustry("Technology");
        orgA = organizationRepository.save(orgA);

        // Create Organization B
        orgB = new Organization();
        orgB.setName("Company B");
        orgB.setIndustry("Finance");
        orgB = organizationRepository.save(orgB);

        // Create Hiring Manager for Org A
        hiringManagerA = createUser("hm-a@companya.com", "hmA", Role.HIRING_MANAGER, orgA);

        // Create Hiring Manager for Org B
        hiringManagerB = createUser("hm-b@companyb.com", "hmB", Role.HIRING_MANAGER, orgB);

        // Create Recruiter for Org A
        recruiterA = createUser("recruiter-a@companya.com", "recruiterA", Role.RECRUITER, orgA);

        // Create Candidate (no organization)
        candidate = createUser("candidate@example.com", "candidate1", Role.CANDIDATE, null);

        // Create job post for Organization A
        authenticateAs(hiringManagerA.getEmail());
        JobPostRequest requestA = createJobPostRequest("Senior Developer at Company A", "OPEN");
        JobPostResponse responseA = jobPostService.createJobPost(requestA, hiringManagerA.getEmail());
        jobPostOrgA = jobPostRepository.findById(responseA.id()).orElseThrow();
        jobPostOrgA.setStatus(JobPostStatus.OPEN); // Make it public
        jobPostOrgA = jobPostRepository.save(jobPostOrgA);

        // Create job post for Organization B
        authenticateAs(hiringManagerB.getEmail());
        JobPostRequest requestB = createJobPostRequest("Data Analyst at Company B", "DRAFT");
        JobPostResponse responseB = jobPostService.createJobPost(requestB, hiringManagerB.getEmail());
        jobPostOrgB = jobPostRepository.findById(responseB.id()).orElseThrow();
    }

    @Test
    void hiringManagerCanOnlySeeOwnOrganizationJobPosts() {
        authenticateAs(hiringManagerA.getEmail());

        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10));

        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).title()).contains("Company A");
        assertThat(jobs.getContent().get(0).title()).doesNotContain("Company B");
    }

    @Test
    void hiringManagerCannotAccessOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getEmail());

        // Try to access job post from Organization B
        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void hiringManagerCannotUpdateOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getEmail());

        JobPostRequest updateRequest = createJobPostRequest("Hacked Title", "OPEN");

        assertThatThrownBy(() -> jobPostService.updateJobPost(
                jobPostOrgB.getId(), updateRequest, hiringManagerA.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void hiringManagerCannotDeleteOtherOrganizationJobPost() {
        authenticateAs(hiringManagerA.getEmail());

        assertThatThrownBy(() -> jobPostService.deleteJobPost(
                jobPostOrgB.getId(), hiringManagerA.getEmail()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found in your organization");
    }

    @Test
    void recruiterCanSeeJobPostsFromSameOrganization() {
        authenticateAs(recruiterA.getEmail());

        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10));

        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).title()).contains("Company A");
    }

    @Test
    void recruiterCannotAccessOtherOrganizationJobPost() {
        authenticateAs(recruiterA.getEmail());

        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void candidateCanOnlySeePublicJobPosts() {
        authenticateAs(candidate.getEmail());

        Page<JobPostResponse> jobs = jobPostService.listJobPosts(PageRequest.of(0, 10));

        // Should only see OPEN job posts
        assertThat(jobs.getContent()).hasSize(1);
        assertThat(jobs.getContent().get(0).id()).isEqualTo(jobPostOrgA.getId());
        assertThat(jobs.getContent().get(0).status()).isEqualTo(JobPostStatus.OPEN);
    }

    @Test
    void candidateCanAccessPublicJobPost() {
        authenticateAs(candidate.getEmail());

        // Can access OPEN job post
        JobPostResponse response = jobPostService.getJobPost(jobPostOrgA.getId());
        assertThat(response.title()).contains("Company A");
    }

    @Test
    void candidateCannotAccessDraftJobPost() {
        authenticateAs(candidate.getEmail());

        // Cannot access DRAFT job post
        assertThatThrownBy(() -> jobPostService.getJobPost(jobPostOrgB.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void listJobPostsByUserRespectsOrganizationBoundaries() {
        authenticateAs(hiringManagerA.getEmail());

        // Can list own organization user's posts
        Page<JobPostResponse> ownPosts = jobPostService.listJobPostsByUser(
                hiringManagerA.getEmail(), PageRequest.of(0, 10));
        assertThat(ownPosts.getContent()).hasSize(1);

        // Cannot list other organization user's posts
        assertThatThrownBy(() -> jobPostService.listJobPostsByUser(
                hiringManagerB.getEmail(), PageRequest.of(0, 10)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("can only view job posts from users in your organization");
    }

    @Test
    void creatingJobPostAutomaticallyAssignsOrganization() {
        authenticateAs(hiringManagerA.getEmail());

        JobPostRequest request = createJobPostRequest("New Job", "DRAFT");
        JobPostResponse response = jobPostService.createJobPost(request, hiringManagerA.getEmail());

        JobPost savedJobPost = jobPostRepository.findById(response.id()).orElseThrow();
        assertThat(savedJobPost.getOrganization()).isNotNull();
        assertThat(savedJobPost.getOrganization().getId()).isEqualTo(orgA.getId());
    }

    @Test
    void candidateCannotCreateJobPost() {
        authenticateAs(candidate.getEmail());

        JobPostRequest request = createJobPostRequest("Candidate Job", "DRAFT");

        assertThatThrownBy(() -> jobPostService.createJobPost(request, candidate.getEmail()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("must belong to an organization");
    }

    @Test
    void publicJobSearchDoesNotLeakDraftPosts() {
        // Create multiple job posts with different statuses
        authenticateAs(hiringManagerA.getEmail());

        JobPostRequest draftRequest = createJobPostRequest("Draft Job A", "DRAFT");
        jobPostService.createJobPost(draftRequest, hiringManagerA.getEmail());

        JobPostRequest openRequest = createJobPostRequest("Open Job A", "OPEN");
        JobPostResponse openResponse = jobPostService.createJobPost(openRequest, hiringManagerA.getEmail());
        JobPost openJob = jobPostRepository.findById(openResponse.id()).orElseThrow();
        openJob.setStatus(JobPostStatus.OPEN);
        jobPostRepository.save(openJob);

        // Candidate searches
        authenticateAs(candidate.getEmail());
        Page<JobPostResponse> results = jobPostService.listJobPosts(PageRequest.of(0, 10));

        // Should only see OPEN jobs
        assertThat(results.getContent())
                .extracting(JobPostResponse::status)
                .containsOnly(JobPostStatus.OPEN);

        assertThat(results.getContent())
                .extracting(JobPostResponse::title)
                .noneMatch(title -> title.contains("Draft"));
    }

    @Test
    void organizationDataIsolationInRepository() {
        // Direct repository test
        Page<JobPost> orgAJobs = jobPostRepository.findByOrganization(
                orgA, PageRequest.of(0, 10));
        Page<JobPost> orgBJobs = jobPostRepository.findByOrganization(
                orgB, PageRequest.of(0, 10));

        assertThat(orgAJobs.getContent()).hasSize(1);
        assertThat(orgBJobs.getContent()).hasSize(1);
        assertThat(orgAJobs.getContent().get(0).getId()).isNotEqualTo(
                orgBJobs.getContent().get(0).getId());
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

    private JobPostRequest createJobPostRequest(String title, String status) {
        return new JobPostRequest(
                title,
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
Step 8: Update Existing Tests
We need to update your existing JobPostServiceImplTest to work with the new organization requirements. Here's what needs to change:

Update src/test/java/com/etalente/backend/service/impl/JobPostServiceImplTest.java:

Add organization setup in the setUp() method:

java
@BeforeEach
void setUp() {
    // Create organization
    organization = new Organization();
    organization.setName("Test Company");
    organization.setIndustry("Technology");
    organization = organizationRepository.save(organization);

    // Create user with organization
    user = new User();
    user.setEmail("test@example.com");
    user.setUsername("testuser");
    user.setRole(Role.HIRING_MANAGER);
    user.setOrganization(organization); // Add this line
    user.setEmailVerified(true);
    user = userRepository.save(user);

    // Authenticate the user
    UserDetails userDetails = User.withUsername(user.getEmail())
            .password("")
            .authorities("ROLE_HIRING_MANAGER")
            .build();
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
}
Add the organization field to the test class:

java
@Autowired
private OrganizationRepository organizationRepository;

private Organization organization;
Step 9: Update Controller Tests
Update src/test/java/com/etalente/backend/controller/JobPostControllerTest.java:

Similar changes needed - ensure test users have organizations:

java
@BeforeEach
void setUp() {
    // Create organization
    Organization organization = new Organization();
    organization.setName("Test Org");
    organization.setIndustry("Tech");
    organization = organizationRepository.save(organization);

    // Create hiring manager with organization
    hiringManager = new User();
    hiringManager.setEmail("hm@test.com");
    hiringManager.setUsername("hiringmanager");
    hiringManager.setRole(Role.HIRING_MANAGER);
    hiringManager.setOrganization(organization); // Add this
    hiringManager.setEmailVerified(true);
    hiringManager = userRepository.save(hiringManager);

    // Create candidate (no organization)
    candidate = new User();
    candidate.setEmail("candidate@test.com");
    candidate.setUsername("candidate");
    candidate.setRole(Role.CANDIDATE);
    candidate.setEmailVerified(true);
    candidate = userRepository.save(candidate);
}
Step 10: Run Tests
Now let's verify everything works:

bash
# Run all tests
./gradlew test

# Run only multi-tenancy tests
./gradlew test --tests "*MultiTenancy*"

# Run organization context tests
./gradlew test --tests "*OrganizationContext*"
Summary of Changes
✅ Database Migration: Added organization_id to job_posts table
✅ Entity Update: Added Organization relationship to JobPost
✅ Security Utility: Created OrganizationContext for easy organization access
✅ Repository: Added organization-filtered query methods
✅ Service Layer: Enforced organization boundaries in all operations
✅ Tests: Comprehensive multi-tenancy security tests

What This Achieves:
✅ All job post queries implicitly filter by organization
✅ Users from one organization cannot access another's data
✅ Security checks prevent cross-organization data leakage
✅ Candidates can only see public (OPEN) job posts
✅ Hiring managers/recruiters see only their organization's posts
✅ Public job listings remain accessible to everyone