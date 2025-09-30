package com.etalente.backend.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_posts")
@EntityListeners(AuditingEntityListener.class)
public class JobPost {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String title;
    private String company;
    private String jobType;
    private String datePosted;
    private String description;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode location;

    private String remote;
    private String salary;
    private String experienceLevel;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode responsibilities;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode qualifications;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode skills;

    @Enumerated(EnumType.STRING)
    private JobPostStatus status = JobPostStatus.DRAFT;;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(String datePosted) {
        this.datePosted = datePosted;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonNode getLocation() {
        return location;
    }

    public void setLocation(JsonNode location) {
        this.location = location;
    }

    public String getRemote() {
        return remote;
    }

    public void setRemote(String remote) {
        this.remote = remote;
    }

    public String getSalary() {
        return salary;
    }

    public void setSalary(String salary) {
        this.salary = salary;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public JsonNode getResponsibilities() {
        return responsibilities;
    }

    public void setResponsibilities(JsonNode responsibilities) {
        this.responsibilities = responsibilities;
    }

    public JsonNode getQualifications() {
        return qualifications;
    }

    public void setQualifications(JsonNode qualifications) {
        this.qualifications = qualifications;
    }

    public JsonNode getSkills() {
        return skills;
    }

    public void setSkills(JsonNode skills) {
        this.skills = skills;
    }

    public JobPostStatus getStatus() {
        return status;
    }

    public void setStatus(JobPostStatus status) {
        this.status = status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}