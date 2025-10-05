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
    RecruiterInvitation sendRecruiterInvitation(RecruiterInvitationRequest request, String inviterEmail);
    void bulkSendRecruiterInvitations(List<RecruiterInvitationRequest> requests, String inviterEmail);
    User acceptInvitation(String token, AcceptInvitationRequest request);
    void revokeInvitation(UUID invitationId, String inviterEmail);
    Page<RecruiterInvitation> getOrganizationInvitations(String userEmail, Pageable pageable);
    Map<String, Object> validateInvitationToken(String token);
}
