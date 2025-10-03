package com.etalente.backend.repository;

import com.etalente.backend.model.InvitationStatus;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.RecruiterInvitation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecruiterInvitationRepository extends JpaRepository<RecruiterInvitation, UUID> {
    Optional<RecruiterInvitation> findByToken(String token);
    
    List<RecruiterInvitation> findByEmail(String email);
    
    Optional<RecruiterInvitation> findByEmailAndOrganizationAndStatus(
            String email, Organization organization, InvitationStatus status);

    Page<RecruiterInvitation> findByOrganization(Organization organization, Pageable pageable);

    @Query("SELECT i FROM RecruiterInvitation i WHERE i.status = 'PENDING' AND i.expiresAt < :now")
    Page<RecruiterInvitation> findExpiredInvitations(LocalDateTime now, Pageable pageable);
}
