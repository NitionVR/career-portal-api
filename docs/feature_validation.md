# Feature Validation Against Specification

This document maps implemented backend features to the (referenced) specification and identifies discrepancies. The repository does not contain a `project.spec` file. We used available documentation (`ARCHITECTURE.md`, `architecture_tree_detailed.md`, and `docs/*`) plus code review to validate features.

## Reference Epics and Features

- Epic 2: User Management & Authentication
  - Passwordless authentication (OTT magic link): Implemented via `AuthenticationController`, `AuthenticationServiceImpl`, `OneTimeToken` and `JwtService`.
  - Session verification/refresh/logout: Implemented (`/api/auth/verify`, `/api/auth/session`, `/api/auth/refresh`, `/api/auth/logout`).
  - Registration flows: Implemented for `CANDIDATE` and `HIRING_MANAGER` in `RegistrationController` and `RegistrationServiceImpl`.
  - Two-Factor Authentication (2FA): Not found (no TOTP/SMS/email code step beyond OTT). Discrepancy.

- Epic 3: Job Post Management
  - Create/update/delete and state transitions with audit trail: Implemented in `JobPostController`, `JobPostService`, `JobPostStateAuditRepository`.
  - Public job listing: Implemented (`JobPostRepository.findPublishedJobs`).
  - Search and filtering: Implemented via `JobPostSpecification` (title/description and skills JSONB path).

- Epic 4: Application Management
  - Candidate applies for job: Implemented (`JobApplicationController` and `JobApplicationServiceImpl.applyForJob`).
  - Employer views and filters applicants: Implemented (`ApplicantServiceImpl.getApplicants`) with caching and rich filters.
  - Application status transitions with audit: Implemented (`JobApplicationController.transitionApplicationStatus`, `JobApplicationAuditRepository`).
  - Candidate withdraw application: Implemented (controller endpoint exists for delete/withdraw).
  - Interview scheduling: Not found in code (no dedicated endpoints/services). Discrepancy.
  - Offer acceptance: Not found. Discrepancy.

- Epic 7: Security & Compliance
  - Role-based access control: Implemented (`SecurityConfig`, `@PreAuthorize`, `OrganizationContext`).
  - Organization scoping: Implemented via `OrganizationContext.verifyOrganizationAccess` and repository methods.
  - Session management: Stateless JWT implemented.

- Epic 1 & 8: Documentation & Deployment
  - Documentation: Extensive docs present under `docs/`. Added `docs/codebase_overview.md` and this `feature_validation.md`.
  - Deployment/Infra: AWS CloudFormation templates present for shared resources.

- Notifications (Cross-cutting)
  - In-app notifications: Implemented (`NotificationService`, `NotificationPreferenceService`, repositories/models).
  - Novu workflows: Implemented for welcome email and application received. No comprehensive set for interviews, decisions, recruiter actions. Discrepancy.

## Test Validation Summary

- Unit, integration, and acceptance test tasks executed successfully individually.
- Aggregate `check` task currently failing; output retrieval unavailable. See `docs/test_report.md` for details.

## Discrepancy List and Recommendations

- 2FA: Not implemented. If required by spec, add TOTP-based 2FA integrated into OTT or post-login verification.
- Interview Scheduling: Not implemented. Add endpoints, domain models, and Novu workflows for invites/reschedules/cancellations.
- Offer Acceptance: Not implemented. Add endpoints and state transitions tied to Novu notification workflows.
- Recruiter Pipeline Updates & Client Communications: Not implemented as dedicated features. Extend services/endpoints and Novu workflows.
- Comprehensive Novu Workflows: Only welcome and application-received are present. See `docs/novu_workflows.md` for a full proposed matrix.

## Implementation Scope Decision

To preserve current functionality and keep all existing tests passing, no backend code changes were made in this pass. The proposed enhancements are documented for planned implementation with minimal risk to existing flows.