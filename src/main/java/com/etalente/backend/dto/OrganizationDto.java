package com.etalente.backend.dto;

import com.etalente.backend.model.Organization;

public class OrganizationDto {
    private String id;
    private String name;
    private String industry;
    private String companyLogoUrl;

    // Constructor
    public OrganizationDto(String id, String name, String industry, String companyLogoUrl) {
        this.id = id;
        this.name = name;
        this.industry = industry;
        this.companyLogoUrl = companyLogoUrl;
    }

    // Factory method
    public static OrganizationDto fromEntity(Organization org) {
        return new OrganizationDto(
            org.getId().toString(),
            org.getName(),
            org.getIndustry(),
            org.getCompanyLogoUrl()
        );
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getCompanyLogoUrl() { return companyLogoUrl; }
    public void setCompanyLogoUrl(String companyLogoUrl) { this.companyLogoUrl = companyLogoUrl; }
}
