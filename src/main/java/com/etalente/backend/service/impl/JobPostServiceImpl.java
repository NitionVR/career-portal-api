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