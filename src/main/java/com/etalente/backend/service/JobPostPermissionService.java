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
        // Check organization first for better error message
        if (user.getOrganization() == null) {
            throw new UnauthorizedException("User must belong to an organization to create job posts");
        }

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