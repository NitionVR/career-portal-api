package com.etalente.backend.service;

import com.etalente.backend.dto.AcceptInvitationRequest;
import com.etalente.backend.dto.RecruiterInvitationRequest;
import com.etalente.backend.model.RecruiterInvitation;
import com.etalente.backend.model.User;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InvitationService {
    RecruiterInvitation sendRecruiterInvitation(RecruiterInvitationRequest request, UUID inviterId);
    void bulkSendRecruiterInvitations(List<RecruiterInvitationRequest> requests, UUID inviterId);
    User acceptInvitation(String token, AcceptInvitationRequest request);
    void revokeInvitation(UUID invitationId, UUID inviterId);
    Page<RecruiterInvitation> getOrganizationInvitations(UUID userId, Pageable pageable);
    Map<String, Object> validateInvitationToken(String token);
}
