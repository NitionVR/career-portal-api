### Etalente Notification System Redundancy Analysis

**Date:** October 15, 2025

**Context:** The Etalente backend currently contains an internal notification system alongside the newly integrated Novu notification orchestration platform. This document identifies redundant components and proposes a strategy for a unified notification pipeline.

**Redundant Components (Internal Notification System):**

The following components are part of an existing internal notification system that largely duplicates functionality now provided by Novu:

*   **Database Schema:**
    *   `src/main/resources/db/migration/V12__Create_notification_tables.sql`: Creates `notifications`, `notification_preferences`, and `notification_delivery_logs` tables.
*   **Data Transfer Objects (DTOs):**
    *   `src/main/java/com/etalente/backend/dto/NotificationRequest.java`
    *   `src/main/java/com/etalente/backend/dto/NotificationResponse.java`
*   **Models/Enums:**
    *   `src/main/java/com/etalente/backend/model/NotificationDeliveryLog.java`
    *   `src/main/java/com.etalente/backend/model/NotificationPreference.java`
    *   `src/main/java/com/etalente/backend/model/NotificationStatus.java`
*   **Controller:**
    *   `src/main/java/com/etalente/backend/controller/NotificationController.java`: Exposes REST endpoints for managing internal notifications (create, get, mark as read, unread count).
*   **Service (Implicit):** There is likely an `NotificationService` implementation that uses these DTOs and models to interact with the internal notification tables.

**Functionality Overlap with Novu:**

*   **Notification Storage & Retrieval:** Novu provides its own activity feed and message storage.
*   **User Preferences:** Novu has robust subscriber preference management.
*   **Delivery Logging:** Novu tracks delivery status and logs.
*   **In-App Notifications:** Novu offers an In-App Inbox SDK that can replace custom in-app notification implementations.

**Recommended Strategy: Consolidate to Novu as the Sole Notification System**

To streamline the architecture, reduce maintenance overhead, and fully leverage Novu's capabilities, it is strongly recommended to transition to Novu as the single source of truth for all notification-related functionality.

**Proposed Actions:**

1.  **Deprecate Internal Components:** Mark the identified redundant DTOs, models, controller, and database tables as deprecated.
2.  **Migrate In-App Notification UI:** If an in-app notification UI exists, refactor it to use Novu's In-App Inbox SDK. This will replace the functionality currently provided by `NotificationController`.
3.  **Remove Redundant Code:** Once the migration is complete and verified, safely remove the deprecated internal notification components.
4.  **Utilize Novu for Preferences:** Manage all user notification preferences directly through Novu's subscriber preferences API.

**Components to Retain/Integrate with Novu:**

*   **`src/main/java/com/etalente/backend/controller/WorkflowController.java`**: This controller, along with `NovuWorkflowService` and `NovuWorkflowCreator`, will be central to triggering and managing Novu workflows from the Etalente backend.
*   **`NovuNotificationService`**: The newly created service will serve as the application's abstraction layer for interacting with Novu.

---

### Update on User Notification Preferences Storage

**Question:** When users set preferences on the frontend, where should that information be stored (your database or Novu)?

**Recommendation:** Store user notification preferences **primarily in Novu**.

**Rationale:**

*   **Single Source of Truth for Delivery:** Novu is the system responsible for sending notifications. Storing preferences directly in Novu ensures it can apply them immediately during workflow execution, guaranteeing users only receive opted-in notifications.
*   **Reduced Duplication & Sync Issues:** Avoids data inconsistencies and the need for complex synchronization mechanisms between your application database and Novu.
*   **Leverage Novu Features:** Fully utilizes Novu's built-in preference management APIs and UI components (e.g., In-App Inbox preference settings).
*   **Scalability:** Novu is optimized for managing notification preferences at scale.

**Implementation Flow:**

1.  **Frontend UI:** User updates preferences in the frontend application.
2.  **Etalente Backend (`NotificationPreferenceService` - New/Modified):**
    *   A dedicated service in your backend (e.g., `NotificationPreferenceService`) receives preference updates from the frontend.
    *   This service then calls the Novu Java SDK (e.g., `novu.getSubscribersHandler().updateSubscriberPreferences()`) to update the preferences for the corresponding user (subscriber) in Novu.
3.  **Novu:** Stores and applies these preferences when triggering workflows.

**Local Database Role:**

*   Your local database should only store minimal preference-related data if absolutely necessary for internal application logic (e.g., a flag indicating if a user has *ever* set preferences, or a timestamp of the last update).
*   Crucially, maintain the mapping between your internal `User` ID and the `subscriberId` used in Novu (which can be your `User` ID converted to a string).