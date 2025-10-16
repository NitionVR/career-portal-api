### Etalente Notification Pipeline Strategy with Novu

**Goal:** To provide a flexible, scalable, and customizable notification system for Etalente, enabling both core application notifications and empowering employers to manage their own notification experiences.

**Core Principles:**

1.  **Event-Driven:** Notifications are triggered by specific events within the Etalente backend (e.g., `user.registered`, `job.applied`, `invitation.sent`).
2.  **Multi-Channel:** Leverage Novu's capabilities to deliver notifications across various channels (Email, In-App, SMS, Push, Chat) based on user preferences and workflow design.
3.  **Separation of Concerns:** Etalente backend focuses on triggering events; Novu handles notification orchestration, templating, and delivery.
4.  **Employer Empowerment:** Provide mechanisms for employers to customize notification content and potentially define their own simple workflows.
5.  **Robustness & Observability:** Utilize Novu's features for retries, fallbacks, and activity feeds for monitoring.

---

#### 1. Application-Level Notifications (Core Workflows)

These are essential notifications managed by the Etalente backend, ensuring critical communications are always sent.

**Implementation:**

*   **Pre-defined Workflows in Novu:** For each core application event that requires a notification, a corresponding workflow will be created in Novu. These workflows will be managed and maintained by the Etalente development team.
    *   **Example Workflows:**
        *   `welcome-email-workflow`: Sends a welcome email to new users.
        *   `magic-link-sent`: Notifies user that a magic link has been sent.
        *   `recruiter-invited`: Notifies a user they've been invited to an organization.
        *   `job-application-received`: Notifies a recruiter/hiring manager of a new job application.
        *   `application-status-changed`: Notifies a candidate when their application status changes.
*   **Programmatic Creation/Update (using `NovuWorkflowCreator`):**
    *   The `NovuWorkflowCreator` class (or a similar utility) will be used to programmatically create and update these core workflows in Novu. This allows for version control of workflows and automation of deployment.
    *   Workflows will be defined in Java code (as demonstrated) and pushed to Novu via the SDK.
    *   **Strategy:** On application startup (e.g., via a `CommandLineRunner` in a non-local profile), the backend can check if these core workflows exist in Novu and create/update them if necessary. This ensures Novu is always in sync with the application's expected notification structure.
*   **Triggering:** The relevant Etalente services will call `novu.triggerEvent()` with the appropriate `workflowId` (e.g., "welcome-email-workflow"), `subscriberId`, and `payload` data.

**Example Integration (Conceptual):**

```java
// In a UserService after a new user registers
@Service
@RequiredArgsConstructor
public class UserService {
    private final Novu novu;
    private final NovuWorkflowCreator novuWorkflowCreator; // To ensure workflow exists

    public User registerUser(RegistrationRequest request) {
        // ... user registration logic ...

        // Ensure the workflow exists before triggering
        // This could be done once on app startup, or on demand.
        // For simplicity, let's assume it's handled by NovuWorkflowInitializer
        // or a similar mechanism on app startup.

        // Trigger welcome email
        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", newUser.getFirstName());
        // Add other variables needed by the template

        TriggerEventRequest triggerRequest = new TriggerEventRequest();
        triggerRequest.setName("welcome-email-workflow"); // The identifier of the workflow in Novu
        triggerRequest.setTo(new co.novu.api.events.pojos.SubscriberRequest(newUser.getId().toString(), newUser.getEmail(), newUser.getFirstName(), newUser.getLastName()));
        triggerRequest.setPayload(payload);

        try {
            novu.triggerEvent(triggerRequest);
        } catch (IOException | NovuNetworkException e) {
            log.error("Failed to trigger welcome email for user {}: {}", newUser.getEmail(), e.getMessage(), e);
        }

        return newUser;
    }
}
```

---

#### 2. Employer-Customizable Notifications

This is where we empower employers to tailor their notification experience.

**Strategy:**

*   **Novu Templates for Customization:** Instead of allowing employers to define full workflows, we can expose specific *message templates* within Novu that employers can customize.
    *   **Example:** A "Job Application Received" workflow might have an email step that uses a template named `employer-job-application-email`.
*   **Employer UI for Template Editing:**
    *   Develop a UI within the Etalente employer dashboard that allows employers to edit the content of *specific, pre-approved Novu templates*.
    *   This UI would interact with Novu's API (via our backend) to `updateTemplate()` (if such an API exists and is exposed by the SDK) or directly update the content of the template.
    *   **Important:** This UI should be carefully designed to prevent malicious code injection and ensure template integrity. It might offer a rich text editor with placeholders (e.g., `{{candidateName}}`, `{{jobTitle}}`) that are dynamically populated by our backend.
*   **Custom Workflows (Advanced - Future):**
    *   For highly advanced employers, a future feature could allow them to define *simple* custom workflows within Novu, perhaps by selecting from a limited set of step types (e.g., "email", "in-app") and configuring their content. This would require a more sophisticated UI and careful validation.
    *   **Consideration:** This is a complex feature. Start with template customization first.
*   **Dynamic Payload Data:** Ensure our backend consistently sends rich payload data to Novu (e.g., `candidateName`, `jobTitle`, `applicationLink`) so that employers can use these variables in their customized templates.

**Implementation Details for Customization:**

1.  **Identify Customizable Templates:** Determine which Novu templates (e.g., `job-application-received-email`) can be exposed for employer customization.
2.  **Backend API for Template Management:** Create backend endpoints (e.g., `/api/employer/notification-templates/{templateId}`) that allow authorized employers to:
    *   `GET` the current content of a template.
    *   `PUT` or `PATCH` to update the content of a template.
    *   These endpoints would use the Novu Java SDK to interact with Novu's template management APIs (if available).
3.  **Frontend UI:** Build a user-friendly interface for employers to view and edit these templates.

---

#### 3. Novu Notifications Pipeline Strategy (High-Level Steps)

1.  **Event Definition:** Clearly define all application events that require notifications (e.g., `user.registered`, `job.applied`).
2.  **Workflow Design (Novu UI / Code):**
    *   For core application notifications, define workflows in Novu (initially via UI, then programmatically using `NovuWorkflowCreator`).
    *   For employer-customizable notifications, define base workflows in Novu that use templates intended for customization.
3.  **Template Creation (Novu UI / Code):**
    *   Create email, in-app, SMS templates in Novu.
    *   Ensure templates use Handlebars syntax for dynamic content (e.g., `{{firstName}}`).
4.  **Integration with Etalente Backend:**
    *   Inject the `Novu` SDK into relevant Etalente services.
    *   Call `novu.triggerEvent()` whenever an application event occurs.
    *   Pass a rich `payload` object containing all necessary data for the templates.
5.  **Subscriber Management:**
    *   Ensure users are registered as subscribers in Novu (e.g., during user registration).
    *   Update subscriber profiles in Novu as user data changes.
6.  **Preference Management:**
    *   Utilize Novu's preference management features to allow users to opt-in/out of certain notification types or channels.
    *   Expose a UI in Etalente for users to manage their preferences.
7.  **Error Handling & Logging:** Implement robust error handling for Novu API calls and log any failures.
8.  **Monitoring:** Leverage Novu's activity feed and analytics to monitor notification delivery and performance.
