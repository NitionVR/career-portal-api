package com.etalente.backend.service;

import com.etalente.backend.dto.OrganizationDto;
import com.etalente.backend.dto.OrganizationMemberDto;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    public OrganizationService(OrganizationRepository organizationRepository,
                              UserRepository userRepository,
                              S3Service s3Service) {
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    /**
     * Update company logo
     */
    public OrganizationDto updateCompanyLogo(String userId, String companyLogoUrl) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization organization = user.getOrganization();
        if (organization == null) {
            throw new ResourceNotFoundException("Organization not found for user");
        }

        // Delete old logo if exists
        if (organization.getCompanyLogoUrl() != null && !organization.getCompanyLogoUrl().isEmpty()) {
            try {
                s3Service.deleteFile(organization.getCompanyLogoUrl());
            } catch (Exception e) {
                logger.error("Failed to delete old company logo: {}", organization.getCompanyLogoUrl(), e);
            }
        }

        organization.setCompanyLogoUrl(companyLogoUrl);
        organization = organizationRepository.save(organization);

        logger.info("Company logo updated for organization: {}", organization.getId());

        return OrganizationDto.fromEntity(organization);
    }

    /**
     * Delete company logo
     */
    public void deleteCompanyLogo(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization organization = user.getOrganization();
        if (organization == null) {
            throw new ResourceNotFoundException("Organization not found for user");
        }

        if (organization.getCompanyLogoUrl() != null) {
            s3Service.deleteFile(organization.getCompanyLogoUrl());
            organization.setCompanyLogoUrl(null);
            organizationRepository.save(organization);

            logger.info("Company logo deleted for organization: {}", organization.getId());
        }
    }

    /**
     * Get organization by user ID
     */
    public OrganizationDto getOrganizationByUserId(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Organization organization = user.getOrganization();
        if (organization == null) {
            throw new ResourceNotFoundException("Organization not found for user");
        }

        return OrganizationDto.fromEntity(organization);
    }

    /**
     * Get all members of an organization.
     */
    @Transactional(readOnly = true)
    public List<OrganizationMemberDto> getOrganizationMembers(UUID organizationId) {
        return userRepository.findByOrganizationId(organizationId).stream()
                .map(user -> new OrganizationMemberDto(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole(),
                        user.getAccountStatus().name(), // Assuming all retrieved members are active for now
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public void updateMemberRole(UUID hiringManagerId, UUID memberId, Role newRole) {
        User hiringManager = userRepository.findById(hiringManagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring manager not found"));

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!hiringManager.getOrganization().equals(member.getOrganization())) {
            throw new SecurityException("Users are not in the same organization");
        }

        member.setRole(newRole);
        userRepository.save(member);
    }

    public void removeMember(UUID hiringManagerId, UUID memberId) {
        User hiringManager = userRepository.findById(hiringManagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Hiring manager not found"));

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        if (!hiringManager.getOrganization().equals(member.getOrganization())) {
            throw new SecurityException("Users are not in the same organization");
        }

        member.setAccountStatus(com.etalente.backend.model.UserAccountStatus.INACTIVE);
        userRepository.save(member);
    }
}
