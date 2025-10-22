package com.etalente.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ApplicantSummaryDto {
    private String id;
    private String candidateName;
    private String jobTitle;
    private String jobId;
    private String profileImageUrl;
    private Integer aiMatchScore;
    private List<String> skills;
    private Integer experienceYears;
    private String location;
    private LocalDateTime applicationDate;
    private String status;

    public ApplicantSummaryDto(String id, String candidateName, String jobTitle, String jobId, String profileImageUrl, Integer aiMatchScore, List<String> skills, Integer experienceYears, String location, LocalDateTime applicationDate, String status) {
        this.id = id;
        this.candidateName = candidateName;
        this.jobTitle = jobTitle;
        this.jobId = jobId;
        this.profileImageUrl = profileImageUrl;
        this.aiMatchScore = aiMatchScore;
        this.skills = skills;
        this.experienceYears = experienceYears;
        this.location = location;
        this.applicationDate = applicationDate;
        this.status = status;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getJobId() {
        return jobId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public Integer getAiMatchScore() {
        return aiMatchScore;
    }

    public List<String> getSkills() {
        return skills;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getApplicationDate() {
        return applicationDate;
    }

    public String getStatus() {
        return status;
    }

    // Setters (if needed, but DTOs are often immutable)
    public void setId(String id) {
        this.id = id;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setAiMatchScore(Integer aiMatchScore) {
        this.aiMatchScore = aiMatchScore;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setApplicationDate(LocalDateTime applicationDate) {
        this.applicationDate = applicationDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
