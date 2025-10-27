package com.etalente.backend;

import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.github.javafaker.Faker;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class TestHelper {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtService jwtService;
    private final EntityManager entityManager;
    private final Faker faker = new Faker();

    public TestHelper(UserRepository userRepository,
                     OrganizationRepository organizationRepository,
                     JwtService jwtService,
                     EntityManager entityManager) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.jwtService = jwtService;
        this.entityManager = entityManager;
    }

    @Transactional
    public void cleanupDatabase() {
        entityManager.clear();

        // Use native queries to delete with proper cascade handling
        entityManager.createNativeQuery("DELETE FROM recruiter_invitations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM job_post_state_audit").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM job_applications").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM job_posts").executeUpdate();
        entityManager.createNativeQuery("UPDATE organizations SET created_by_id = NULL").executeUpdate();
        entityManager.createNativeQuery("UPDATE users SET organization_id = NULL, invited_by_id = NULL").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM organizations").executeUpdate();
        entityManager.createNativeQuery("DELETE FROM users").executeUpdate();

        entityManager.flush();
        entityManager.clear();
    }

    public String createUserAndGetJwt(String email, Role role) {
        User user = createUser(email, role);
        return jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                false,
                user.getUsername()
        );
    }

    public String generateJwtForUser(User user) {
        return jwtService.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name(),
                false,
                user.getUsername()
        );
    }

    public User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        user.setUsername("testuser-" + UUID.randomUUID());
        user.setFirstName(faker.name().firstName());
        user.setLastName(faker.name().lastName());
        user.setCompanyName(faker.company().name());
        user.setIndustry(faker.company().industry());

        // Save the user first to get a persistent instance with an ID
        User savedUser = userRepository.save(user);

        if (role == Role.HIRING_MANAGER || role == Role.RECRUITER) {
            // Now create the organization with the persistent user
            Organization org = new Organization();
            org.setName(savedUser.getCompanyName());
            org.setIndustry(savedUser.getIndustry());
            org.setCreatedBy(savedUser); // Use the persistent user
            Organization savedOrg = organizationRepository.save(org);

            // Associate the organization with the user and save again
            savedUser.setOrganization(savedOrg);
            return userRepository.save(savedUser);
        }

        return savedUser;
    }

    public User createUser(String email, Role role, Organization organization) {
        User user = new User();
        user.setEmail(email);
        user.setRole(role);
        user.setUsername("testuser-" + UUID.randomUUID());
        user.setFirstName(faker.name().firstName());
        user.setLastName(faker.name().lastName());
        user.setCompanyName(organization.getName());
        user.setIndustry(organization.getIndustry());
        user.setOrganization(organization);

        return userRepository.save(user);
    }
}
