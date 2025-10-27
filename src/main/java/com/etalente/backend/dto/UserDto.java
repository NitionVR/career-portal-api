package com.etalente.backend.dto;

import com.etalente.backend.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UserDto {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String profileImageUrl;
    private boolean isNewUser;

    // New fields for flattened profile data
    private JsonNode basics;
    private List<JsonNode> work;
    private List<JsonNode> education;
    private List<JsonNode> skills;
    private List<JsonNode> projects;
    private List<JsonNode> awards;
    private List<JsonNode> certificates;
    private List<JsonNode> interests;
    private List<JsonNode> languages;
    private List<JsonNode> publications;
    private List<JsonNode> references;
    private List<JsonNode> volunteer;

    // Constructor
    public UserDto(String id, String email, String firstName, String lastName,
                  String role, String profileImageUrl, boolean isNewUser,
                  JsonNode basics, List<JsonNode> work, List<JsonNode> education,
                  List<JsonNode> skills, List<JsonNode> projects, List<JsonNode> awards,
                  List<JsonNode> certificates, List<JsonNode> interests, List<JsonNode> languages,
                  List<JsonNode> publications, List<JsonNode> references, List<JsonNode> volunteer) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.profileImageUrl = profileImageUrl;
        this.isNewUser = isNewUser;
        this.basics = basics;
        this.work = work;
        this.education = education;
        this.skills = skills;
        this.projects = projects;
        this.awards = awards;
        this.certificates = certificates;
        this.interests = interests;
        this.languages = languages;
        this.publications = publications;
        this.references = references;
        this.volunteer = volunteer;
    }

    // Factory method
    public static UserDto fromEntity(User user) {
        ObjectMapper objectMapper = new ObjectMapper(); // Create ObjectMapper locally for now

        JsonNode profile = user.getProfile();
        JsonNode basics = objectMapper.createObjectNode();
        List<JsonNode> work = new ArrayList<>();
        List<JsonNode> education = new ArrayList<>();
        List<JsonNode> skills = new ArrayList<>();
        List<JsonNode> projects = new ArrayList<>();
        List<JsonNode> awards = new ArrayList<>();
        List<JsonNode> certificates = new ArrayList<>();
        List<JsonNode> interests = new ArrayList<>();
        List<JsonNode> languages = new ArrayList<>();
        List<JsonNode> publications = new ArrayList<>();
        List<JsonNode> references = new ArrayList<>();
        List<JsonNode> volunteer = new ArrayList<>();

        if (profile != null && !profile.isNull()) {
            // Extract basics
            basics = Optional.ofNullable(profile.get("basics")).orElse(objectMapper.createObjectNode());

            // Extract arrays
            work = extractJsonNodeList(profile, "work", objectMapper);
            education = extractJsonNodeList(profile, "education", objectMapper);
            skills = extractJsonNodeList(profile, "skills", objectMapper);
            projects = extractJsonNodeList(profile, "projects", objectMapper);
            awards = extractJsonNodeList(profile, "awards", objectMapper);
            certificates = extractJsonNodeList(profile, "certificates", objectMapper);
            interests = extractJsonNodeList(profile, "interests", objectMapper);
            languages = extractJsonNodeList(profile, "languages", objectMapper);
            publications = extractJsonNodeList(profile, "publications", objectMapper);
            references = extractJsonNodeList(profile, "references", objectMapper);
            volunteer = extractJsonNodeList(profile, "volunteer", objectMapper);
        }

        return new UserDto(
            user.getId().toString(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().name(),
            user.getProfileImageUrl(),
            user.isNewUser(),
            basics, work, education, skills, projects, awards,
            certificates, interests, languages, publications, references, volunteer
        );
    }

    // Helper method to extract a list of JsonNodes from an ArrayNode
    private static List<JsonNode> extractJsonNodeList(JsonNode parentNode, String fieldName, ObjectMapper objectMapper) {
        JsonNode node = parentNode.get(fieldName);
        if (node != null && node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    @JsonProperty("isNewUser")
    public boolean isNewUser() { return isNewUser; }
    public void setNewUser(boolean newUser) { isNewUser = newUser; }

    public JsonNode getBasics() { return basics; }
    public void setBasics(JsonNode basics) { this.basics = basics; }

    public List<JsonNode> getWork() { return work; }
    public void setWork(List<JsonNode> work) { this.work = work; }

    public List<JsonNode> getEducation() { return education; }
    public void setEducation(List<JsonNode> education) { this.education = education; }

    public List<JsonNode> getSkills() { return skills; }
    public void setSkills(List<JsonNode> skills) { this.skills = skills; }

    public List<JsonNode> getProjects() { return projects; }
    public void setProjects(List<JsonNode> projects) { this.projects = projects; }

    public List<JsonNode> getAwards() { return awards; }
    public void setAwards(List<JsonNode> awards) { this.awards = awards; }

    public List<JsonNode> getCertificates() { return certificates; }
    public void setCertificates(List<JsonNode> certificates) { this.certificates = certificates; }

    public List<JsonNode> getInterests() { return interests; }
    public void setInterests(List<JsonNode> interests) { this.interests = interests; }

    public List<JsonNode> getLanguages() { return languages; }
    public void setLanguages(List<JsonNode> languages) { this.languages = languages; }

    public List<JsonNode> getPublications() { return publications; }
    public void setPublications(List<JsonNode> publications) { this.publications = publications; }

    public List<JsonNode> getReferences() { return references; }
    public void setReferences(List<JsonNode> references) { this.references = references; }

    public List<JsonNode> getVolunteer() { return volunteer; }
    public void setVolunteer(List<JsonNode> volunteer) { this.volunteer = volunteer; }
}
