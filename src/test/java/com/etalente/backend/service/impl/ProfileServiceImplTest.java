package com.etalente.backend.service.impl;

import com.etalente.backend.exception.ResourceNotFoundException;
import com.etalente.backend.model.User;
import com.etalente.backend.repository.UserRepository;
import com.etalente.backend.service.impl.ProfileServiceImpl;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    private Faker faker;
    private User testUser;
    private String userEmail;

    @BeforeEach
    void setUp() {
        faker = new Faker();
        userEmail = faker.internet().emailAddress();
        testUser = new User();
        testUser.setEmail(userEmail);
        testUser.setFirstName(faker.name().firstName());
        testUser.setLastName(faker.name().lastName());
    }

    @Test
    void updateProfile_shouldUpdateAllFields_whenUserExists() {
        // Given
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));

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
        profileService.updateProfile(userEmail, profileData);

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
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(testUser));

        Map<String, String> profileData = new HashMap<>();
        String newFirstName = faker.name().firstName();
        profileData.put("firstName", newFirstName);
        profileData.put("lastName", null); // lastName is null

        // When
        profileService.updateProfile(userEmail, profileData);

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
        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.empty());
        Map<String, String> profileData = new HashMap<>();
        profileData.put("firstName", "test");

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> profileService.updateProfile(userEmail, profileData));
        verify(userRepository, never()).save(any(User.class));
    }
}
