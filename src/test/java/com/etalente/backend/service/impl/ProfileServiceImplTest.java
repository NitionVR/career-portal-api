package com.etalente.backend.service.impl;

import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.security.JwtService;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.etalente.backend.service.impl.ProfileServiceImpl;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private Faker faker;
    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        userId = UUID.randomUUID();
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail(faker.internet().emailAddress());
        testUser.setFirstName(faker.name().firstName());
        testUser.setLastName(faker.name().lastName());
    }

    @Test
    void updateProfile_shouldUpdateAllFields_whenUserExists() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Map<String, String> profileData = new HashMap<>();
        String newFirstName = faker.name().firstName();
        String newLastName = faker.name().lastName();
        String newContactNumber = faker.phoneNumber().cellPhone();
        String newSummary = faker.lorem().sentence();

        profileData.put("firstName", newFirstName);
        profileData.put("lastName", newLastName);
        profileData.put("contactNumber", newContactNumber);
        profileData.put("summary", newSummary);

        // When
        profileService.updateProfile(userId, profileData);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(newFirstName, savedUser.getFirstName());
        assertEquals(newLastName, savedUser.getLastName());
        assertEquals(newContactNumber, savedUser.getContactNumber());
        assertEquals(newSummary, savedUser.getSummary());
        assertTrue(savedUser.isProfileComplete());
    }

    @Test
    void updateProfile_shouldUpdatePartialFields_whenSomeAreNull() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        Map<String, String> profileData = new HashMap<>();
        String newFirstName = faker.name().firstName();
        profileData.put("firstName", newFirstName);
        profileData.put("lastName", null); // lastName is null

        // When
        profileService.updateProfile(userId, profileData);

        // Then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals(newFirstName, savedUser.getFirstName());
        assertNull(savedUser.getLastName()); // Should be updated to null
        assertNull(savedUser.getContactNumber()); // Should remain null
        assertNull(savedUser.getSummary()); // Should remain null
    }

    @Test
    void updateProfile_shouldThrowResourceNotFoundException_whenUserDoesNotExist() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        Map<String, String> profileData = new HashMap<>();
        profileData.put("firstName", "test");

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> profileService.updateProfile(userId, profileData));
        verify(userRepository, never()).save(any(User.class));
    }
}