# Implementation Plan: Employer Features & Profile Enhancements

This document breaks down the implementation of the "Backend API Requirements v2" into a checklist of actionable steps.

---

### Goal 1: P0 - Enhance Employer Dashboard Endpoint

**Objective:** Update the `GET /api/job-posts/my-posts` endpoint to include applicant counts and the company logo URL in the response.

- [x] **Database:** Create a new Flyway migration (`V16_...`) to add a `viewed_by_employer` column to the `job_applications` table.
- [x] **JPA Entity:** Update the `JobApplication.java` entity to include the new `viewedByEmployer` field.
- [x] **Repository:** Update `JobApplicationRepository.java` to add two new methods:
    - `int countByJobPostId(UUID jobPostId);`
    - `int countByJobPostIdAndViewedByEmployerFalse(UUID jobPostId);`
- [x] **DTO:** Modify `JobPostResponse.java` to include the new fields: `applicantsCount`, `newApplicantsCount`, and `companyLogoUrl`.
- [x] **Service Logic:** Update `JobPostServiceImpl.java` to:
    - Use the new repository methods to calculate applicant counts.
    - Fetch the `companyLogoUrl` from the job post's associated organization.
    - Populate the new fields in the `JobPostResponse`.
- [x] **Testing:** Create or update tests to verify that the `my-posts` endpoint correctly returns the new fields with accurate data.

---

### Goal 2: P1 & P2 - Implement S3 File Uploads

**Objective:** Set up the core infrastructure for AWS S3 uploads and implement the profile picture and company logo features.

- [x] **Dependencies:** Add the AWS S3 SDK dependencies (`s3` and `sts`) to `build.gradle`.
- [x] **Configuration:**
    - [x] Add AWS S3 and file upload properties to `application.yml`.
    - [x] Create the `S3Config.java` class to configure the `S3Client` and `S3Presigner` beans.
- [ ] **Core S3 Service:** Create the `S3Service.java` class to handle the logic for generating pre-signed URLs and deleting files.
- [ ] **Database:** Create a new Flyway migration (`V17_...`) to:
    - [ ] Add `profile_image_url` to the `users` table.
    - [x] Add `company_logo_url` to the `organizations` table.
- [ ] **JPA Entities:**
    - [ ] Update `User.java` to include the `profileImageUrl` field.
    - [x] Update `Organization.java` to include the `companyLogoUrl` field.
- [ ] **DTOs:**
    - [ ] Create `UploadUrlRequest.java`.
    - [ ] Create `UploadUrlResponse.java`.
    - [ ] Create `UpdateAvatarRequest.java`.
    - [ ] Create `UpdateCompanyLogoRequest.java`.
- [ ] **Profile Picture Feature (P1):**
    - [ ] Create a new `ProfileService.java` (or update the existing one) with methods to `updateProfileImage` and `deleteProfileImage`.
    - [ ] Update `ProfileController.java` to add the three new endpoints for getting an upload URL, updating the avatar, and deleting it.
    - [ ] Update `UserDto.java` to include the `profileImageUrl`.
- [ ] **Company Logo Feature (P2):**
    - [ ] Create a new `OrganizationService.java` with methods to `updateCompanyLogo` and `deleteCompanyLogo`.
    - [ ] Create a new `OrganizationController.java` with the three new endpoints for the company logo.
    - [ ] Update `OrganizationDto.java` to include the `companyLogoUrl`.
- [ ] **Testing:** Add new integration tests for all new endpoints to ensure they are secure and function correctly.
