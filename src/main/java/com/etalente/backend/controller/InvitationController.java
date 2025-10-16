package com.etalente.backend.controller;

import com.etalente.backend.dto.AcceptInvitationRequest;
import com.etalente.backend.dto.RecruiterInvitationDto;
import com.etalente.backend.dto.RecruiterInvitationRequest;
import com.etalente.backend.model.RecruiterInvitation;
import com.etalente.backend.model.User;
import com.etalente.backend.service.AuthenticationService;
import com.etalente.backend.service.InvitationService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final AuthenticationService authenticationService;

    public InvitationController(InvitationService invitationService,
                               AuthenticationService authenticationService) {
        this.invitationService = invitationService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/recruiter")
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<RecruiterInvitationDto> inviteRecruiter(
            @Valid @RequestBody RecruiterInvitationRequest request,
            Authentication authentication) {
        UUID inviterId = UUID.fromString(authentication.getName());
        RecruiterInvitation invitation = invitationService.sendRecruiterInvitation(request, inviterId);
        RecruiterInvitationDto dto = new RecruiterInvitationDto(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getExpiresAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/bulk-recruiter")
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<Map<String, String>> inviteRecruiterBulk(
            @Valid @RequestBody List<RecruiterInvitationRequest> requests,
            Authentication authentication) {
        UUID inviterId = UUID.fromString(authentication.getName());
        invitationService.bulkSendRecruiterInvitations(requests, inviterId);
        return ResponseEntity.ok(Map.of("message", "Invitations processed"));
    }

    @GetMapping("/validate/{token}")
    public ResponseEntity<Map<String, Object>> validateInvitation(@PathVariable String token) {
        Map<String, Object> validationResult = invitationService.validateInvitationToken(token);
        return ResponseEntity.ok(validationResult);
    }

    @PostMapping("/accept/{token}")
    public ResponseEntity<Map<String, String>> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequest request) {
        User user = invitationService.acceptInvitation(token, request);
        String jwt = authenticationService.generateJwtForUser(user);
        return ResponseEntity.ok(Map.of("token", jwt, "message", "Invitation accepted successfully"));
    }

    @PatchMapping("/{invitationId}/revoke")
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public ResponseEntity<Void> revokeInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication) {
        UUID inviterId = UUID.fromString(authentication.getName());
        invitationService.revokeInvitation(invitationId, inviterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('HIRING_MANAGER')")
    public Page<RecruiterInvitationDto> listOrganizationInvitations(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return invitationService.getOrganizationInvitations(userId, pageable)
                .map(invitation -> new RecruiterInvitationDto(
                        invitation.getId(),
                        invitation.getEmail(),
                        invitation.getStatus(),
                        invitation.getCreatedAt(),
                        invitation.getExpiresAt()
                ));
    }
}