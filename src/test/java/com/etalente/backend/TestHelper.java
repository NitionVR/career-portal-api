package com.etalente.backend;

import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.OrganizationRepository;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.github.javafaker.Faker;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TestHelper {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtService jwtService;
    private final Faker faker = new Faker();

    public TestHelper(UserRepository userRepository, OrganizationRepository organizationRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.jwtService = jwtService;
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

        if (role == Role.HIRING_MANAGER) {
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
}
