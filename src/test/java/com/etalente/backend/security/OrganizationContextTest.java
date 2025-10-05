package com.etalente.backend.security;

import com.etalente.backend.BaseIntegrationTest;
import com.etalente.backend.exception.UnauthorizedException;
import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrganizationContextTest extends BaseIntegrationTest {

    @Autowired
    private OrganizationContext organizationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private Organization org1;
    private Organization org2;
    private User hiringManager1;
    private User hiringManager2;
    private User candidate;

    @BeforeEach
    void setUp() {
        // Create organizations
        org1 = new Organization();
        org1.setName("Company A");
        org1.setIndustry("Tech");
        org1 = organizationRepository.save(org1);

        org2 = new Organization();
        org2.setName("Company B");
        org2.setIndustry("Finance");
        org2 = organizationRepository.save(org2);

        // Create hiring manager for org1
        hiringManager1 = new User();
        hiringManager1.setEmail("hm1@companya.com");
        hiringManager1.setUsername("hm1");
        hiringManager1.setRole(Role.HIRING_MANAGER);
        hiringManager1.setOrganization(org1);
        hiringManager1.setEmailVerified(true);
        hiringManager1 = userRepository.save(hiringManager1);

        // Create hiring manager for org2
        hiringManager2 = new User();
        hiringManager2.setEmail("hm2@companyb.com");
        hiringManager2.setUsername("hm2");
        hiringManager2.setRole(Role.HIRING_MANAGER);
        hiringManager2.setOrganization(org2);
        hiringManager2.setEmailVerified(true);
        hiringManager2 = userRepository.save(hiringManager2);

        // Create candidate (no organization)
        candidate = new User();
        candidate.setEmail("candidate@example.com");
        candidate.setUsername("candidate1");
        candidate.setRole(Role.CANDIDATE);
        candidate.setEmailVerified(true);
        candidate = userRepository.save(candidate);
    }

    @Test
    void shouldGetCurrentOrganizationForHiringManager() {
        authenticateAs(hiringManager1.getEmail());

        UUID orgId = organizationContext.getCurrentOrganizationId();
        assertThat(orgId).isEqualTo(org1.getId());

        Organization org = organizationContext.getCurrentOrganization();
        assertThat(org.getName()).isEqualTo("Company A");
    }

    @Test
    void shouldReturnNullOrganizationForCandidate() {
        authenticateAs(candidate.getEmail());

        UUID orgId = organizationContext.getCurrentOrganizationId();
        assertThat(orgId).isNull();

        Organization org = organizationContext.getCurrentOrganization();
        assertThat(org).isNull();
    }

    @Test
    void shouldThrowExceptionWhenRequireOrganizationForCandidate() {
        authenticateAs(candidate.getEmail());

        assertThatThrownBy(() -> organizationContext.requireOrganizationId())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("must belong to an organization");
    }

    @Test
    void shouldVerifyOrganizationAccess() {
        authenticateAs(hiringManager1.getEmail());

        // Should not throw for same organization
        assertThatCode(() -> organizationContext.verifyOrganizationAccess(org1.getId()))
                .doesNotThrowAnyException();

        // Should throw for different organization
        assertThatThrownBy(() -> organizationContext.verifyOrganizationAccess(org2.getId()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("don't have access");
    }

    @Test
    void shouldCheckBelongsToOrganization() {
        authenticateAs(hiringManager1.getEmail());

        assertThat(organizationContext.belongsToOrganization(org1.getId())).isTrue();
        assertThat(organizationContext.belongsToOrganization(org2.getId())).isFalse();
        assertThat(organizationContext.belongsToOrganization(null)).isFalse();
    }

    @Test
    void shouldIdentifyCandidate() {
        authenticateAs(candidate.getEmail());
        assertThat(organizationContext.isCandidate()).isTrue();

        authenticateAs(hiringManager1.getEmail());
        assertThat(organizationContext.isCandidate()).isFalse();
    }

    private void authenticateAs(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}