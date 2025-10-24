# Etalente Backend Codebase Overview

This document provides a high-level overview of the Etalente backend architecture, key modules, flows, external integrations, and testing setup. It is intended to help developers, QA, and stakeholders understand the system, locate components quickly, and maintain consistency.

## Architecture Summary

- Stack: Spring Boot (Java), PostgreSQL, Testcontainers (testing), AWS SDK (S3, SES), JWT-based stateless auth, Novu notifications.
- Structure: Layered architecture with Controllers → Services → Repositories → Models.
- Security: Custom `JwtAuthenticationFilter` and `SecurityConfig` with stateless sessions and role-based access (`Role`: CANDIDATE, HIRING_MANAGER, RECRUITER).
- Notifications: In-app notification models/services plus Novu integration for email/in-app workflows.
- File Upload: S3 presigned URLs via `S3Service` and `UploadProperties`.
- Docs: Additional domain-specific docs exist in `docs/` (auth, registration, notifications, uploads).

## Key Packages and Responsibilities

- `com.etalente.backend.controller`
  - `AuthenticationController`: Passwordless login using one-time tokens (OTT). Verify token returns session JWT; refresh and logout endpoints.
  - `RegistrationController`: Completes registration for `CANDIDATE` and `HIRING_MANAGER`; token validation.
  - `JobPostController`: CRUD and state transitions for job posts with audit trail; public listing.
  - `JobApplicationController`: Candidate applies; HIRING_MANAGER/RECRUITER transitions application status; listing endpoints.
  - `OrganizationController`: Organization management and asset updates.
  - `ProfileController`: Profile updates and completion.
  - `NotificationController`: In-app notification endpoints and preferences.
  - `WorkflowController`: Utility endpoints to trigger or synchronize workflows (including Novu).

- `com.etalente.backend.service`
  - Core service interfaces: `AuthenticationService`, `RegistrationService`, `JobPostService`, `JobApplicationService`, `ProfileService`, `OrganizationService`, `InvitationService`, `NotificationService`, `NotificationPreferenceService`, `NovuNotificationService`, `S3Service`.
  - Implementations: Found under `service.impl/` covering business logic, validations, orchestration, and integrations.
  - Notable implementations:
    - `RegistrationServiceImpl`: Handles user creation and Novu welcome notifications.
    - `JobApplicationServiceImpl`: Apply for job; triggers Novu `application-received` workflow; transitions application statuses.
    - `ApplicantServiceImpl`: Employer-side filtering and search over applicants/applications (caching, specs, auditing).
    - `NovuNotificationServiceImpl`: Sends welcome notifications via Novu.

- `com.etalente.backend.integration.novu`
  - `NovuClientConfig`: Configures Novu SDK client using `novu.api-key` and backend URL.
  - `NovuSubscriberService`: Creates/updates subscribers in Novu.
  - `NovuWorkflowCreator`: Defines and syncs workflows (e.g., `welcome-email-workflow`) and groups.
  - `NovuWorkflowService`: Asynchronously triggers workflows by name with subscriber and payload.

- `com.etalente.backend.security`
  - `SecurityConfig`: Stateless security, CORS settings, public endpoints, role protections.
  - `JwtAuthenticationFilter`: Validates JWT, sets authentication with `ROLE_*` authorities.
  - `JwtService`: JWT utilities (parse claims, validation, expiration).
  - `OrganizationContext`: Helpers to get current user, check organization access, and role checks.

- `com.etalente.backend.model`
  - Core domain models: `User`, `Organization`, `JobPost`, `JobApplication`.
  - State/Audit: `JobPostStatus`, `JobPostStateAudit`, `JobApplicationStatus`, `JobApplicationAudit`, `StateTransition`.
  - Auth: `OneTimeToken`, `RegistrationToken`.
  - Notifications: `Notification`, `NotificationDeliveryLog`, `NotificationPreference`, `NotificationStatus`.

- `com.etalente.backend.repository`
  - JPA repositories for all models plus Specifications for advanced filtering:
    - `JobPostSpecification` and `JobApplicationSpecification` for search/filters.

- `com.etalente.backend.config`
  - `JobPostSeeder`: Seeds job posts using JSON files under `src/main/resources/job_seed_jsons/`.
  - `S3Config`, `MailConfig`, `JpaConfig`, `CorsProperties`, `UploadProperties`.

## External Integrations

- AWS S3: Uploads via presigned URLs; CORS requirements documented in `docs/s3_cors_requirements.md`.
- AWS SES: Email sender abstraction plugged by `EmailService` and `EmailSender`.
- Novu: Notifications via workflows; see `docs/novu_java_sdk_documentation.md` and `docs/novu_notification_strategy.md`.

## Testing Overview

- Unit tests: Service and utility coverage; run with `gradlew test`.
- Integration tests: Testcontainers PostgreSQL with mocked AWS/email.
- Acceptance tests: Karate-based E2E API scenarios.
- Aggregate check: `gradlew check` (may aggregate all testing and verification tasks).

## Configuration and Environment

- Files: `src/main/resources/application.yml`, `application-ses.yml`.
- Test config: `src/test/resources/application-test.yml` and `application-test.properties`.
- Important env vars: See `docs/required_environment_variables.md`.

## Request Flow Examples

- Candidate applies for a job:
  1. `JobApplicationController.applyForJob` receives request.
  2. `JobApplicationServiceImpl.applyForJob` validates, persists, audits.
  3. Triggers Novu workflow `application-received` for job poster with payload (applicant name/email, job title/company, application ID/date).

- Registration and welcome notification:
  1. `RegistrationController` complete registration endpoint.
  2. `RegistrationServiceImpl` creates user, organization (if HM), profile basics.
  3. `NovuNotificationServiceImpl.sendWelcomeNotification` triggers `welcome-email-workflow`.

## Security and Authorization

- Roles:
  - `CANDIDATE`: Can apply for jobs, manage own profile.
  - `HIRING_MANAGER`: Manage job posts and applications for their organization.
  - `RECRUITER`: Similar to HM with recruiter-specific flows and invitations.
- Access control enforced via `@PreAuthorize` and `OrganizationContext` checks.

## Auditing and State Machines

- Job posts and applications capture state transitions with audit entries (`JobPostStateAudit`, `JobApplicationAudit`).
- Transition endpoints accept `StateTransitionRequest` / `ApplicationTransitionRequest` to move between valid statuses.

## Known Documents for Further Detail

- `ARCHITECTURE.md` and `architecture_tree_detailed.md` (root): Deep tree and epics overview.
- `docs/magic_link_ott_refactor.md`: Details around passwordless auth flow.
- `docs/registration_api_contract.md`: Registration payloads and responses.
- `docs/job-post-permissions.md`: Permissions and role-based access for job posts.
- `docs/novu_java_sdk_documentation.md` and `docs/novu_notification_strategy.md`: Novu integration specifics.

## Maintenance Notes

- Keep Novu workflows in sync using `NovuWorkflowCreator` if templates or groups change.
- Review Testcontainers and mocked integrations when updating dependencies.
- Monitor deprecation warnings (e.g., Mockito self-attach) for future JDK compatibility.