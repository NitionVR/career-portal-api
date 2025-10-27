package com.etalente.backend.service.impl;

import com.etalente.backend.dto.AcceptInvitationRequest;
import com.etalente.backend.dto.RecruiterInvitationRequest;
import com.etalente.backend.exception.BadRequestException;
import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.*;
import com.etalente.backend.repository.*;
import com.etalente.backend.service.EmailService;
import com.etalente.backend.service.InvitationService;
import com.etalente.backend.service.TokenStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvitationServiceImpl implements InvitationService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final RecruiterInvitationRepository invitationRepository;
    private final EmailService emailService;
    private final Optional<TokenStore> tokenStore;

    @Value("${invitation-link-url}")
    private String invitationLinkUrl;

    @Value("${invitation-expiry-hours:72}")
    private int invitationExpiryHours;

    public InvitationServiceImpl(UserRepository userRepository,
                                OrganizationRepository organizationRepository,
                                RecruiterInvitationRepository invitationRepository,
                                EmailService emailService,
                                @Autowired(required = false) Optional<TokenStore> tokenStore) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.invitationRepository = invitationRepository;
        this.emailService = emailService;
        this.tokenStore = tokenStore;
    }

    @Override
    public RecruiterInvitation sendRecruiterInvitation(RecruiterInvitationRequest request, UUID inviterId) {
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify the inviter is a hiring manager
        if (inviter.getRole() != Role.HIRING_MANAGER) {
            throw new UnauthorizedException("Only hiring managers can invite recruiters");
        }

        // Get or create organization
        Organization organization = inviter.getOrganization();
        if (organization == null) {
            organization = createOrganizationForUser(inviter);
            inviter = userRepository.findById(inviterId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }
        final Organization finalOrganization = organization;

        // Check if user already exists
        userRepository.findByEmail(request.email()).ifPresent(existingUser -> {
            if (existingUser.getOrganization() != null &&
                existingUser.getOrganization().getId().equals(finalOrganization.getId())) {
                throw new BadRequestException("User is already a member of your organization");
            }
        });

        // Check for existing pending invitation
        invitationRepository.findByEmailAndOrganizationAndStatus(
                request.email(), organization, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new BadRequestException("An invitation is already pending for this email");
                });

        // Create invitation
        RecruiterInvitation invitation = new RecruiterInvitation();
        invitation.setEmail(request.email());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setOrganization(organization);
        invitation.setInvitedBy(inviter);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusHours(invitationExpiryHours));

        RecruiterInvitation savedInvitation = invitationRepository.save(invitation);

        // Store token for test purposes
        tokenStore.ifPresent(store -> store.addToken(request.email(), savedInvitation.getToken()));

        // Send email
        String invitationLink = invitationLinkUrl + "?token=" + savedInvitation.getToken() + "&action=accept_invitation";
        emailService.sendRecruiterInvitation(
                request.email(),
                inviter.getFirstName() + " " + inviter.getLastName(),
                organization.getName(),
                invitationLink,
                request.personalMessage()
        );

        return savedInvitation;
    }

    @Override
    public void bulkSendRecruiterInvitations(List<RecruiterInvitationRequest> requests, UUID inviterId) {
        for (RecruiterInvitationRequest request : requests) {
            try {
                sendRecruiterInvitation(request, inviterId);
            } catch (Exception e) {
                // Log the error for individual invitation failures
                // Depending on requirements, you might collect these errors and return them
                // For now, we'll just log and continue
                System.err.println("Failed to send invitation to " + request.email() + ": " + e.getMessage());
            }
        }
    }

    @Override
    public User acceptInvitation(String token, AcceptInvitationRequest request) {
        RecruiterInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid invitation token"));

        // Validate invitation
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("This invitation has already been " + invitation.getStatus().name().toLowerCase());
        }

        if (LocalDateTime.now().isAfter(invitation.getExpiresAt())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new BadRequestException("This invitation has expired");
        }

        // Check username uniqueness
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already taken");
        }

        // Create or update user
        User user = userRepository.findByEmail(invitation.getEmail())
                .orElse(new User());

        user.setEmail(invitation.getEmail());
        user.setUsername(request.username());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setContactNumber(request.contactNumber());
        user.setRole(Role.RECRUITER);
        user.setOrganization(invitation.getOrganization());
        user.setInvitedBy(invitation.getInvitedBy());
        user.setInvitedAt(LocalDateTime.now());
        user.setEmailVerified(true);
        user.setProfileComplete(true);

        user = userRepository.save(user);

        // Update invitation
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationRepository.save(invitation);

        return user;
    }

    @Override
    public void revokeInvitation(UUID invitationId, UUID inviterId) {
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RecruiterInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        // Verify the inviter has permission
        if (!invitation.getInvitedBy().getId().equals(inviter.getId()) &&
            !invitation.getOrganization().getCreatedBy().getId().equals(inviter.getId())) {
            throw new UnauthorizedException("You don't have permission to revoke this invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Can only revoke pending invitations");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitationRepository.save(invitation);
    }

    @Override
    public Page<RecruiterInvitation> getOrganizationInvitations(UUID userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOrganization() == null) {
            throw new BadRequestException("You are not part of any organization");
        }

        return invitationRepository.findByOrganization(user.getOrganization(), pageable);
    }

    private Organization createOrganizationForUser(User user) {
        Organization organization = new Organization();
        organization.setName(user.getCompanyName());
        organization.setIndustry(user.getIndustry());
        organization.setCreatedBy(user);
        organization = organizationRepository.save(organization);

        user.setOrganization(organization);
        userRepository.save(user);

        return organization;
    }

    @Override
    public Map<String, Object> validateInvitationToken(String token) {
        try {
            RecruiterInvitation invitation = invitationRepository.findByToken(token)
                    .orElseThrow(() -> new BadRequestException("Invalid invitation token"));

            boolean isValid = invitation.getStatus() == InvitationStatus.PENDING &&
                             LocalDateTime.now().isBefore(invitation.getExpiresAt());

            return Map.of(
                "valid", isValid,
                "email", invitation.getEmail(),
                "organization", invitation.getOrganization().getName(),
                "invitedBy", invitation.getInvitedBy().getFirstName() + " " +
                            invitation.getInvitedBy().getLastName(),
                "status", invitation.getStatus().name(),
                "expiresAt", invitation.getExpiresAt().toString()
            );
        } catch (Exception e) {
            return Map.of("valid", false, "error", e.getMessage());
        }
    }

    @Override
    public void resendInvitation(UUID invitationId, UUID resenderId) {
        RecruiterInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        User resender = userRepository.findById(resenderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (resender.getRole() != Role.HIRING_MANAGER) {
            throw new UnauthorizedException("Only hiring managers can resend invitations");
        }

        if (!resender.getOrganization().getId().equals(invitation.getOrganization().getId())) {
            throw new UnauthorizedException("You are not in the same organization as the invitation");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Can only resend pending invitations");
        }

        // Reset token and expiry
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(LocalDateTime.now().plusHours(invitationExpiryHours));
        invitationRepository.save(invitation);

        // Store token for test purposes
        tokenStore.ifPresent(store -> store.addToken(invitation.getEmail(), invitation.getToken()));

        // Send email
        String invitationLink = invitationLinkUrl + "?token=" + invitation.getToken() + "&action=accept_invitation";
        emailService.sendRecruiterInvitation(
                invitation.getEmail(),
                invitation.getInvitedBy().getFirstName() + " " + invitation.getInvitedBy().getLastName(),
                invitation.getOrganization().getName(),
                invitationLink,
                "We are sending you this invitation again."
        );
    }
}
