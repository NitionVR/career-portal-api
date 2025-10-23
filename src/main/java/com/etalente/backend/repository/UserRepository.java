package com.etalente.backend.repository;

import com.etalente.backend.model.Organization;
import com.etalente.backend.model.Role;
import com.etalente.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u LEFT JOIN FETCH u.organization WHERE u.email = :email")
    Optional<User> findByEmail(@org.springframework.data.repository.query.Param("email") String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByOrganizationAndRole(Organization organization, Role role);
    List<User> findByOrganizationId(UUID organizationId);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}
