package com.etalente.backend.security;

import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
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

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof String)) {
            throw new UnauthorizedException("Invalid authentication principal");
        }

        UUID userId = UUID.fromString((String) principal);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
//...
    public User getCurrentUserOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
                return null;
            }

            Object principal = authentication.getPrincipal();
            if (!(principal instanceof String)) {
                return null;
            }

            UUID userId = UUID.fromString((String) principal);
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if there is an authenticated user
     */
    public boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken);
    }

    /**
     * Check if the current user is a candidate (safely handles unauthenticated users)
     * @return true if user is authenticated and is a candidate or if no user is authenticated
     */
    public boolean isCandidateOrUnauthenticated() {
        User user = getCurrentUserOrNull();
        return user == null || user.getRole() == Role.CANDIDATE;
    }
}