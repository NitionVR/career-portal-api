# Novu Notification Workflows: Analysis and Enhancements

This document reviews the current Novu integration and proposes comprehensive workflows for CANDIDATE, HIRING MANAGER, and RECRUITER actions, including trigger points, payloads, and configuration guidelines.

## Current Implementation Summary

- Client Configuration: `NovuClientConfig` uses `novu.api-key` with backend base URL.
- Subscribers: `NovuSubscriberService` creates/updates subscribers keyed by user ID, with email/phone as available.
- Workflow Definitions: `NovuWorkflowCreator` currently defines `welcome-email-workflow` (email step with template and preferences) and manages notification groups.
- Workflow Triggering: `NovuWorkflowService.triggerWorkflow(name, subscriber, payload)` asynchronously triggers workflows.
- Existing Triggers:
  - Registration: `NovuNotificationServiceImpl.sendWelcomeNotification(user)` triggers `welcome-email-workflow`.
  - Application Submission: `JobApplicationServiceImpl.applyForJob` triggers `application-received` to job poster with applicant details.

## Enhancement Goals

- Expand workflows across channels (email + in-app, and optional SMS) with robust payload schemas.
- Standardize naming conventions for workflows and topics.
- Document trigger points at service/controller layers to ensure reliable delivery.
- Introduce preference handling and groups for opt-in/opt-out per role.

## Proposed Workflow Matrix

- Candidate Workflows
  - `candidate-application-submitted`: Already covered via `application-received` to HM; add candidate confirmation to candidate subscriber.
  - `candidate-interview-scheduled`: Notify candidate of scheduled interview details; send calendar attachment via email; in-app notification.
  - `candidate-interview-rescheduled`: Notify candidate of reschedule changes.
  - `candidate-interview-cancelled`: Notify candidate and provide next steps.
  - `candidate-offer-extended`: Notify candidate of offer details.
  - `candidate-offer-accepted`: Notify employers and recruiters; confirmation to candidate.
  - `candidate-offer-declined`: Notify employers and recruiters.

- Hiring Manager Workflows
  - `hm-application-received`: Already in place via `application-received`.
  - `hm-candidate-reviewed`: Notify HM of review completion (team feedback aggregator), optional.
  - `hm-interview-feedback-needed`: Prompt HM to provide feedback post-interview.
  - `hm-hiring-decision-required`: Notify HM to finalize decision.

- Recruiter Workflows
  - `recruiter-candidate-sourced`: Notify recruiter/HM when a new candidate is sourced into pipeline.
  - `recruiter-pipeline-updated`: Notify stakeholders of pipeline stage changes.
  - `recruiter-client-update`: Notify client organization contacts of updates/actions.

## Trigger Points in Code

- Registration (welcome): `RegistrationServiceImpl.completeRegistration` → `NovuNotificationService.sendWelcomeNotification(user)`.
- Application Submission: `JobApplicationServiceImpl.applyForJob` → `NovuWorkflowService.triggerWorkflow('application-received', hmSubscriber, payload)`.
- Application Status Transitions: `JobApplicationController.transitionApplicationStatus` → extend service to trigger appropriate workflows (e.g., interview scheduled/rescheduled/cancelled, offer extended/accepted/declined).
- Candidate Confirmation: Extend `applyForJob` to also notify candidate (`candidate-application-submitted`).

## Suggested Payload Schemas

Standardize payloads for each workflow. Examples:

- `candidate-application-submitted`
  - `{ "applicationId": string, "jobTitle": string, "company": string, "submittedAt": string, "candidate": { "id": string, "name": string, "email": string } }`

- `hm-application-received`
  - `{ "applicationId": string, "jobTitle": string, "applicant": { "id": string, "name": string, "email": string }, "submittedAt": string }`

- `candidate-interview-scheduled`
  - `{ "applicationId": string, "jobTitle": string, "company": string, "interview": { "id": string, "type": string, "start": string, "end": string, "location": string, "timezone": string }, "organizer": { "id": string, "name": string } }`

- `candidate-offer-extended`
  - `{ "applicationId": string, "jobTitle": string, "company": string, "offer": { "id": string, "title": string, "compensation": string, "validUntil": string }, "contact": { "name": string, "email": string } }`

## Novu SDK Features to Leverage

- Workflows: Multi-step (email, in-app, SMS); enable `validatePayload` with JSON schema.
- Notification Groups: Use `getOrCreateNotificationGroup` to isolate role-based preferences.
- Topics: Group subscribers by organization or job ID for batch updates.
- Overrides: Customize channel provider settings per trigger when needed.
- Preferences: Respect `NotificationPreference` in-app settings and align with Novu preferences.

## Configuration and Naming Conventions

- Naming: Use kebab-case for workflow IDs (e.g., `candidate-interview-scheduled`).
- Groups: `candidates`, `hiring-managers`, `recruiters`.
- Topics: `org-{organizationId}`, `job-{jobId}`.
- Environment: `novu.api-key` and backend URL must be set in config; document in `docs/required_environment_variables.md`.

## Reliability and Delivery

- Ensure subscriber existence before triggering: Use `NovuSubscriberService` upsert before each trigger.
- Async execution: `NovuWorkflowService` runs triggers asynchronously; consider retries and delivery logging alignment with `NotificationDeliveryLog`.
- Idempotency: Include `applicationId` or `interviewId` and timestamps in payload for deduplication in workflows.

## Implementation Roadmap (Non-breaking)

- Add triggers in `JobApplicationServiceImpl` for candidate confirmation and status-based workflows.
- Introduce new workflow definitions via `NovuWorkflowCreator` for interview and offer events.
- Align in-app notification service with Novu events for consistent UX.
- Expand test coverage for workflow triggers using mocked Novu client.

## Maintenance Notes

- Keep workflow templates in Novu synchronized when changing payload schemas.
- Audit and log delivery outcomes; reconcile with `NotificationDeliveryLog` for observability.
- Document any added endpoints and DTOs in `docs/frontend_api_updates/*` for frontend integration.