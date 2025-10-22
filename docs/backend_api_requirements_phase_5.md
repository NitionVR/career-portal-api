### Backend API Requirements for Employer Features - Implementation Plan

This plan outlines the step-by-step implementation for the new employer-facing features, integrating with existing services and models while minimizing regressions.

---

#### **1. Applicant Management Module (`/api/applicants`)**

**1.1. `GET /api/applicants` - Paginated and Filterable List**

*   **Step 1.1.1: Define `ApplicantSummaryDto`**
    *   Create a new DTO class `ApplicantSummaryDto.java` in `src/main/java/com/etalente/backend/dto`.
    *   Include fields: `id`, `candidateName`, `jobTitle`, `jobId`, `profileImageUrl`, `aiMatchScore`, `skills`, `experienceYears`, `location`, `applicationDate`, `status`.
    *   Ensure proper constructors, getters, and setters. Use Lombok annotations if consistent with project style, otherwise manual.

*   **Step 1.1.2: Create `ApplicantService` Interface and Implementation**
    *   Create `ApplicantService.java` interface in `src/main/java/com/etalente/backend/service`.
    *   Create `ApplicantServiceImpl.java` in `src/main/java/com/etalente/backend/service/impl`.
    *   Add a method `Page<ApplicantSummaryDto> getApplicants(Pageable pageable, String search, String skillSearch, String jobId, List<String> statuses, Integer experienceMin, List<String> education, String location, Integer aiMatchScoreMin, UUID organizationId)` to the interface and its implementation.
    *   This service will orchestrate data retrieval from `JobApplicationRepository`, `JobPostRepository`, and `UserRepository`.

*   **Step 1.1.3: Enhance `JobApplicationRepository` (or create `ApplicantRepository`)**
    *   Add custom query methods or Spring Data JPA Specifications to `JobApplicationRepository` to handle filtering by `jobId`, `status`, `skills`, `experienceMin`, `education`, `location`, and `aiMatchScoreMin`.
    *   Consider using `Criteria API` for complex dynamic queries if specifications become too cumbersome.
    *   For `skills` and `location` (if stored as JSON), explore `PostgreSQL JSONB` query capabilities via Hibernate/JPA.

*   **Step 1.1.4: Create `ApplicantController`**
    *   Create `ApplicantController.java` in `src/main/java/com/etalente/backend/controller`.
    *   Map `GET /api/applicants` to a method that calls `ApplicantService.getApplicants`.
    *   Apply `@PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")` for access control.
    *   Inject `Authentication` object to get the current user's `organizationId`.

*   **Step 1.1.5: Unit and Integration Tests**
    *   Write unit tests for `ApplicantServiceImpl` covering all filtering and pagination logic.
    *   Write integration tests for `ApplicantController` using `MockMvc` to verify endpoint behavior and security.

**1.2. `POST /api/applicants/bulk-action`**

*   **Step 1.2.1: Define `BulkActionRequest` DTO**
    *   Create `BulkActionRequest.java` in `src/main/java/com/etalente/backend/dto`.
    *   Include fields: `applicationIds`, `action` (enum), `payload`.
    *   Define nested DTOs for `payload` as needed (e.g., `UpdateStatusPayload`, `AddTagsPayload`, `ExportPayload`).

*   **Step 1.2.2: Extend `ApplicantService` for Bulk Actions**
    *   Add a method `void performBulkAction(BulkActionRequest request, UUID organizationId)` to `ApplicantService` interface and implementation.
    *   Implement logic to handle each `action` type:
        *   `UPDATE_STATUS`: Update `JobApplication` status. Integrate with `NovuNotificationService` if `sendNotification` is true.
        *   `ADD_TAGS`: Requires a new `Tag` entity/table or a JSONB field on `JobApplication` to store tags. Implement logic to add tags.
        *   `EXPORT`: Implement an `ExportService` (e.g., `CsvExportService`, `PdfExportService`) to generate files. Return a URL or stream the file.

*   **Step 1.2.3: Create `ExportService` (if needed)**
    *   If export functionality is complex, create a dedicated `ExportService` to handle CSV/PDF generation.

*   **Step 1.2.4: Extend `ApplicantController`**
    *   Map `POST /api/applicants/bulk-action` to a method that calls `ApplicantService.performBulkAction`.
    *   Apply `@PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")`.

*   **Step 1.2.5: Unit and Integration Tests**
    *   Write tests for `ApplicantService` covering all bulk action types.
    *   Write integration tests for `ApplicantController`.

**1.3. `POST /api/applicants/{applicationId}/notes`**

*   **Step 1.3.1: Define `CollaborationNoteRequest` DTO**
    *   Create `CollaborationNoteRequest.java` in `src/main/java/com/etalente/backend/dto`.
    *   Include fields: `note`, `mentionedUserIds`.

*   **Step 1.3.2: Create `CollaborationNote` Model and Repository**
    *   Create `CollaborationNote.java` entity in `src/main/java/com/etalente/backend/model` (fields: `id`, `applicationId`, `userId` (creator), `note`, `createdAt`).
    *   Create `CollaborationNoteRepository.java` in `src/main/java/com/etalente/backend/repository`.
    *   Add Flyway migration script to create the `collaboration_notes` table.

*   **Step 1.3.3: Extend `ApplicantService` for Notes**
    *   Add a method `CollaborationNoteDto addCollaborationNote(UUID applicationId, CollaborationNoteRequest request, UUID userId)` to `ApplicantService`.
    *   Implement logic to save the note.
    *   Integrate with `NovuNotificationService` to notify `mentionedUserIds`.

*   **Step 1.3.4: Extend `ApplicantController`**
    *   Map `POST /api/applicants/{applicationId}/notes` to a method that calls `ApplicantService.addCollaborationNote`.
    *   Apply `@PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")`.

*   **Step 1.3.5: Unit and Integration Tests**
    *   Write tests for `ApplicantService` and `CollaborationNoteRepository`.
    *   Write integration tests for `ApplicantController`.

**1.4. `GET /api/applicants/analytics`**

*   **Step 1.4.1: Define `AnalyticsDto` DTO**
    *   Create `AnalyticsDto.java` in `src/main/java/com/etalente/backend/dto`.
    *   Include fields: `totalApplications`, `newApplications`, `interviewStage`, `averageTimeToFill`, `statusDistribution`, `sourceDistribution`, `genderDistribution`.

*   **Step 1.4.2: Extend `ApplicantService` for Analytics**
    *   Add a method `AnalyticsDto getApplicantAnalytics(String period, UUID jobId, UUID organizationId)` to `ApplicantService`.
    *   Implement complex database queries (e.g., using `COUNT`, `GROUP BY` with `JobApplication`, `JobPost`, `User` entities) to aggregate data.
    *   Consider a dedicated `AnalyticsRepository` if queries become very complex.

*   **Step 1.4.3: Extend `ApplicantController`**
    *   Map `GET /api/applicants/analytics` to a method that calls `ApplicantService.getApplicantAnalytics`.
    *   Apply `@PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")`.

*   **Step 1.4.4: Unit and Integration Tests**
    *   Write tests for `ApplicantService` covering analytics calculations.
    *   Write integration tests for `ApplicantController`.

---

#### **2. Organization & Members Module (`/api/organization`)**

**2.1. `GET /api/organization/members`**

*   **Step 2.1.1: Define `OrganizationMemberDto` DTO**
    *   Create `OrganizationMemberDto.java` in `src/main/java/com/etalente/backend/dto`.
    *   Include fields: `id`, `email`, `firstName`, `lastName`, `role`, `status`, `joinedDate`.

*   **Step 2.1.2: Extend `OrganizationService`**
    *   Add a method `List<OrganizationMemberDto> getOrganizationMembers(UUID organizationId)` to `OrganizationService` interface and implementation.
    *   This method will query `UserRepository` for users associated with the given `organizationId`.
    *   Map `User` entities to `OrganizationMemberDto`.

*   **Step 2.1.3: Extend `OrganizationController`**
    *   Map `GET /api/organization/members` to a method that calls `OrganizationService.getOrganizationMembers`.
    *   Apply `@PreAuthorize("hasAnyRole('HIRING_MANAGER', 'RECRUITER')")`.

*   **Step 2.1.4: Unit and Integration Tests**
    *   Write tests for `OrganizationService` and `OrganizationController`.

**2.2. `PUT /api/organization/members/{memberId}/role`**

*   **Step 2.2.1: Define `UpdateMemberRoleRequest` DTO**
    *   Create `UpdateMemberRoleRequest.java` in `src/main/java/com/etalente/backend/dto`.
    *   Include field: `newRole` (enum `Role`).

*   **Step 2.2.2: Extend `OrganizationService`**
    *   Add a method `OrganizationMemberDto updateMemberRole(UUID memberId, Role newRole, UUID callingUserId)` to `OrganizationService`.
    *   Implement logic to fetch the `User` by `memberId`, update their `role`, and save.
    *   Add authorization logic within the service to ensure `callingUserId` has permission (e.g., is a HIRING_MANAGER and not trying to change their own role or the role of another HIRING_MANAGER if not allowed).

*   **Step 2.2.3: Extend `OrganizationController`**
    *   Map `PUT /api/organization/members/{memberId}/role` to a method that calls `OrganizationService.updateMemberRole`.
    *   Apply `@PreAuthorize("hasRole('HIRING_MANAGER')")`.

*   **Step 2.2.4: Unit and Integration Tests**
    *   Write tests for `OrganizationService` covering role updates and authorization.
    *   Write integration tests for `OrganizationController`.

**2.3. `DELETE /api/organization/members/{memberId}`**

*   **Step 2.3.1: Extend `OrganizationService`**
    *   Add a method `void removeMember(UUID memberId, UUID callingUserId)` to `OrganizationService`.
    *   Implement logic to fetch the `User` by `memberId`.
    *   Instead of physical deletion, set the user's `organization` to `null` or introduce a `status` field (e.g., `ACTIVE`, `DISABLED`) in the `User` model and set it to `DISABLED`. Given `OrganizationMemberDto` has a `status` field, adding a `status` field to `User` and setting it to `DISABLED` is preferred.
    *   Add authorization logic within the service.

*   **Step 2.3.2: Extend `OrganizationController`**
    *   Map `DELETE /api/organization/members/{memberId}` to a method that calls `OrganizationService.removeMember`.
    *   Apply `@PreAuthorize("hasRole('HIRING_MANAGER')")`.

*   **Step 2.3.3: Unit and Integration Tests**
    *   Write tests for `OrganizationService` covering member removal and authorization.
    *   Write integration tests for `OrganizationController`.

**2.4. `POST /api/invitations/resend/{invitationId}`**

*   **Step 2.4.1: Extend `InvitationService`**
    *   Add a method `void resendInvitation(UUID invitationId, UUID callingUserId)` to `InvitationService` interface and implementation.
    *   Implement logic to fetch the `RecruiterInvitation` by `invitationId`.
    *   Update `expiresAt` to a new future date.
    *   Set `status` to `PENDING` if it was `EXPIRED`.
    *   Re-send the invitation email using `EmailService`.
    *   Add authorization logic to ensure `callingUserId` has permission.

*   **Step 2.4.2: Extend `InvitationController`**
    *   Map `POST /api/invitations/resend/{invitationId}` to a method that calls `InvitationService.resendInvitation`.
    *   Apply `@PreAuthorize("hasRole('HIRING_MANAGER')")`.

*   **Step 2.4.3: Unit and Integration Tests**
    *   Write tests for `InvitationService` and `InvitationController`.

---

#### **3. Existing Endpoints to Verify/Update**

**3.1. `GET /api/invitations`**

*   **Step 3.1.1: Verification**
    *   Confirm that `InvitationController.listOrganizationInvitations` and its associated `RecruiterInvitationDto` already provide `id`, `email`, `createdAt`, `status`.
    *   **Conclusion:** No changes required.

**3.2. `POST /api/invitations/recruiter` & `POST /api/invitations/bulk-recruiter`**

*   **Step 3.2.1: Update `RecruiterInvitationRequest` DTO**
    *   Modify `RecruiterInvitationRequest.java` in `src/main/java/com/etalente/backend/dto` to add an optional `String personalMessage` field.

*   **Step 3.2.2: Update `InvitationService`**
    *   Modify `InvitationService.sendRecruiterInvitation` and `bulkSendRecruiterInvitations` methods to accept and pass the `personalMessage` to the `EmailService` for inclusion in the invitation email.

*   **Step 3.2.3: Update `EmailService`**
    *   Modify `EmailService.sendRecruiterInvitation` to accept `personalMessage` and include it in the email template.

*   **Step 3.2.4: Update `InvitationController`**
    *   Ensure `InvitationController` methods (`inviteRecruiter`, `inviteRecruiterBulk`) correctly receive the updated `RecruiterInvitationRequest` and pass the `personalMessage` to the service.

*   **Step 3.2.5: Unit and Integration Tests**
    *   Update existing tests for `InvitationService` and `InvitationController` to cover the new `personalMessage` field.

---

This plan provides a comprehensive approach to implementing the new backend API requirements. Each step is designed to be atomic and testable, ensuring a robust and maintainable solution.