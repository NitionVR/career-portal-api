package com.etalente.backend.config;

import com.etalente.backend.dto.JobPostRequest;
import com.etalente.backend.dto.JobPostResponse;
import com.etalente.backend.dto.LocationDto;
import com.etalente.backend.dto.SkillDto;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.JobPostRepository;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.service.JobPostService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Profile("local") // Only run this seeder in the 'local' profile
public class JobPostSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(JobPostSeeder.class);

    private final JobPostService jobPostService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JobPostRepository jobPostRepository;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourcePatternResolver;

    // Cache for created users to avoid re-creating for the same company
    private final Map<String, User> companyHiringManagers = new HashMap<>();

    public JobPostSeeder(JobPostService jobPostService,
                         UserRepository userRepository,
                         OrganizationRepository organizationRepository,
                         JobPostRepository jobPostRepository,
                         ObjectMapper objectMapper) {
        this.jobPostService = jobPostService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.jobPostRepository = jobPostRepository;
        this.objectMapper = objectMapper;
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
    }

    @Override
    public void run(String... args) {
        logger.info("Starting JobPostSeeder...");
        try {
            seedJobPosts();
            logger.info("JobPostSeeder finished successfully.");
        } catch (Exception e) {
            logger.error("JobPostSeeder failed to complete: {}", e.getMessage(), e);
        }
    }

    private User getOrCreateHiringManagerAndOrganization(String companyName) {
        // Normalize company name for consistent keys
        String normalizedCompanyName = companyName.trim().toLowerCase();

        // Check cache first
        if (companyHiringManagers.containsKey(normalizedCompanyName)) {
            return companyHiringManagers.get(normalizedCompanyName);
        }

        // Check if organization already exists
        Optional<Organization> existingOrg = organizationRepository.findByName(companyName);
        final Organization organization; // Declare as final
        User hiringManager;

        if (existingOrg.isPresent()) {
            organization = existingOrg.get();
            logger.info("Organization already exists: {}", organization.getName());
            // Try to find an existing hiring manager for this organization
            hiringManager = userRepository.findByOrganizationAndRole(organization, Role.HIRING_MANAGER)
                    .orElseGet(() -> {
                        // If no hiring manager found, create one
                        logger.info("Creating hiring manager for existing organization: {}", organization.getName());
                        return createHiringManager(companyName, organization);
                    });
        } else {
            // Create new Organization
            logger.info("Creating new organization: {}", companyName);
            Organization newOrganization = new Organization(); // Use a new variable for creation
            newOrganization.setName(companyName);
            newOrganization.setIndustry("General"); // Default industry
            organization = organizationRepository.save(newOrganization);

            // Create new Hiring Manager for the new organization
            logger.info("Creating hiring manager for new organization: {}", companyName);
            hiringManager = createHiringManager(companyName, organization);

            // Update organization with createdBy user
            organization.setCreatedBy(hiringManager);
            organizationRepository.save(organization);
        }

        companyHiringManagers.put(normalizedCompanyName, hiringManager);
        return hiringManager;
    }

    private User createHiringManager(String companyName, Organization organization) {
        String email = companyName.toLowerCase().replaceAll("[^a-z0-9]", "") + "@example.com";
        String username = companyName.toLowerCase().replaceAll("[^a-z0-9]", "") + "hm";

        // Check if user with this email already exists
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            logger.info("User with email {} already exists, reusing.", email);
            return existingUser.get();
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setRole(Role.HIRING_MANAGER);
        user.setFirstName(companyName + " Hiring");
        user.setLastName("Manager");
        user.setEmailVerified(true);
        user.setProfileComplete(true);
        user.setOrganization(organization);
        return userRepository.save(user);
    }


    @Transactional
    protected void seedJobPosts() throws IOException {
        logger.info("Seeding job posts...");

        Resource[] resources = resourcePatternResolver.getResources("classpath:job_seed_jsons/*.json");

        for (Resource resource : resources) {
            if (!resource.exists() || !resource.isReadable() || (resource.getFilename() != null && resource.getFilename().equals("job_12.json"))) {
                logger.warn("Skipping resource: {}", resource.getFilename());
                continue;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                // Read into a generic map to handle different key names
                @SuppressWarnings("unchecked")
                Map<String, Object> jobData = objectMapper.readValue(inputStream, Map.class);

                // Manually construct the JobPostRequest, handling old ("type", "experience") and new keys
                String jobType = (String) jobData.getOrDefault("jobType", jobData.get("type"));
                String experienceLevel = (String) jobData.getOrDefault("experienceLevel", jobData.get("experience"));

                JobPostRequest request = new JobPostRequest(
                        (String) jobData.get("title"),
                        (String) jobData.get("company"),
                        jobType,
                        (String) jobData.get("description"),
                        objectMapper.convertValue(jobData.get("location"), LocationDto.class),
                        (String) jobData.get("remote"),
                        (String) jobData.get("salary"),
                        experienceLevel,
                        objectMapper.convertValue(jobData.get("responsibilities"), new TypeReference<List<String>>() {}),
                        objectMapper.convertValue(jobData.get("qualifications"), new TypeReference<List<String>>() {}),
                        objectMapper.convertValue(jobData.get("skills"), new TypeReference<List<SkillDto>>() {})
                );

                User hiringManager = getOrCreateHiringManagerAndOrganization(request.company());

                if (!jobPostRepository.existsByTitleAndCompany(request.title(), request.company())) {
                    logger.info("Attempting to create and publish job post: {}", request.title());
                    JobPostResponse createdJobPost = jobPostService.createJobPost(request, hiringManager.getId());
                    logger.info("Job post created with ID: {}", createdJobPost.id());

                    var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + hiringManager.getRole().name()));
                    var authentication = new UsernamePasswordAuthenticationToken(hiringManager.getId().toString(), null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    try {
                        jobPostService.publishJobPost(createdJobPost.id(), hiringManager.getId());
                        logger.info("Job post published: {}", createdJobPost.title());
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                } else {
                    logger.info("Job post already exists, skipping: {}", request.title());
                }
            } catch (Exception e) {
                logger.error("Failed to seed job post from {}: {}", resource.getFilename(), e.getMessage(), e);
            }
        }
    }
}

