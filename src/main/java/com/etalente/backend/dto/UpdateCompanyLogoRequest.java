package com.etalente.backend.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public class UpdateCompanyLogoRequest {

    @NotBlank(message = "Company logo URL is required")
    @URL(protocol = "https", message = "Company logo URL must be a valid HTTPS URL")
    private String companyLogoUrl;

    public UpdateCompanyLogoRequest() {}

    public UpdateCompanyLogoRequest(String companyLogoUrl) {
        this.companyLogoUrl = companyLogoUrl;
    }

    public String getCompanyLogoUrl() {
        return companyLogoUrl;
    }

    public void setCompanyLogoUrl(String companyLogoUrl) {
        this.companyLogoUrl = companyLogoUrl;
    }
}
