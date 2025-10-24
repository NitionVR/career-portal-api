## Recruiter Invitation Workflow - Backend Flow Explanation

This document outlines the backend API endpoints and logic involved in the recruiter invitation workflow, from sending an invitation to a new recruiter completing their registration. It also covers other related invitation management functionalities.

---

### **1. Sending an Invitation**

*   **Endpoint:** `POST /api/invitations/recruiter` (single) or `POST /api/invitations/bulk-recruiter` (multiple)
*   **Authentication:** Requires authentication. The authenticated user must have the `HIRING_MANAGER` role.
*   **Request Type:** `POST`
*   **Request Body (`RecruiterInvitationRequest`):

    ```typescript
    interface RecruiterInvitationRequest {
      email: string;
      personalMessage?: string; // Optional message to include in the invitation email
    }
    ```

*   **Response (`RecruiterInvitationDto` for single, `Map<String, String>` for bulk):

    ```typescript
    interface RecruiterInvitationDto {
      id: string;
      email: string;
      status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED';
      createdAt: string; // ISO 8601
      expiresAt: string; // ISO 8601
    }
    ```

*   **Backend Logic (`InvitationServiceImpl.sendRecruiterInvitation`):**
    1.  **Authorization:** Verifies the `inviterId` belongs to a `HIRING_MANAGER`.
    2.  **Organization Check:** Retrieves the inviter's organization. If the inviter (Hiring Manager) doesn't have an organization yet, one is created for them.
    3.  **Existing User/Invitation Check:** Checks for existing user with the invited email in the same organization or a pending invitation. Throws `BadRequestException` if conflicts.
    4.  **Create Invitation:** A `RecruiterInvitation` entity is created with a unique UUID `token`, `PENDING` status, and an `expiresAt` timestamp.
    5.  **Save Invitation:** The `RecruiterInvitation` is saved to the database.
    6.  **Send Email:** An invitation email is sent via `EmailService.sendRecruiterInvitation`. The email contains a link to the frontend's invitation acceptance page, including the generated `token`.

--- 

### **2. Validating an Invitation Token**

*   **Endpoint:** `GET /api/invitations/validate/{token}`
*   **Authentication:** Public (no authentication required)
*   **Request Type:** `GET`
*   **Path Parameters:**
    *   `token`: `string` (UUID) - The invitation token received in the email link.
*   **Response (`Map<String, Object>`):

    ```typescript
    interface InvitationValidationResponse {
      valid: boolean;
      email: string;
      organization: string; // Name of the inviting organization
      invitedBy: string;    // Full name of the inviter
      status: 'PENDING' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED';
      expiresAt: string;    // ISO 8601
      error?: string;       // Present if valid is false
    }
    ```

*   **Backend Logic (`InvitationServiceImpl.validateInvitationToken`):**
    1.  **Token Lookup:** Finds the `RecruiterInvitation` by the provided `token`. Throws `BadRequestException` if not found.
    2.  **Status & Expiry Check:** Verifies if the invitation `status` is `PENDING` and if `expiresAt` is in the future.
    3.  **Response:** Returns details about the invitation and its validity.

*   **Frontend Interaction:** When a user clicks an invitation link (e.g., `https://your-frontend.com/accept-invitation?token=YOUR_TOKEN`), the frontend should extract the token and call this endpoint to display invitation details (e.g., who invited them, which organization) and allow the user to proceed with acceptance.

--- 

### **3. Accepting an Invitation & Completing Registration**

*   **Endpoint:** `POST /api/invitations/accept/{token}`
*   **Authentication:** Public (no authentication required, but creates an authenticated session upon success)
*   **Request Type:** `POST`
*   **Path Parameters:**
    *   `token`: `string` (UUID) - The invitation token.
*   **Request Body (`AcceptInvitationRequest`):

    ```typescript
    interface AcceptInvitationRequest {
      username: string;
      firstName: string;
      lastName: string;
      contactNumber?: string;
      // Add other required registration fields for a Recruiter
    }
    ```

*   **Response (`Map<String, String>`):

    ```typescript
    interface InvitationAcceptanceResponse {
      token: string; // JWT for the newly registered/accepted user
      message: string; // Success message
    }
    ```

*   **Backend Logic (`InvitationServiceImpl.acceptInvitation`):**
    1.  **Invitation Validation:** Finds and validates the `RecruiterInvitation` (must be `PENDING` and not expired).
    2.  **Username Uniqueness:** Checks if the provided `username` is already taken.
    3.  **User Creation/Update:** Creates a new `User` (or updates if email exists) with `RECRUITER` role, associates with the inviting `Organization`, and sets `emailVerified` and `profileComplete` to `true`.
    4.  **Update Invitation:** The `RecruiterInvitation` `status` is updated to `ACCEPTED`.
    5.  **Generate JWT:** A new JWT is generated for the user.
    6.  **Response:** Returns the generated JWT and a success message.

*   **Frontend Interaction:** After the user fills out their registration details, the frontend calls this endpoint. Upon success, the frontend should store the received JWT (to authenticate the user) and redirect them to the application's dashboard.

--- 

### **4. Revoking an Invitation**

*   **Endpoint:** `PATCH /api/invitations/{invitationId}/revoke`
*   **Authentication:** Requires authentication. The authenticated user must have the `HIRING_MANAGER` role.
*   **Request Type:** `PATCH`
*   **Path Parameters:**
    *   `invitationId`: `string` (UUID) - The ID of the invitation to revoke.
*   **Response:** `204 No Content`
*   **Backend Logic (`InvitationServiceImpl.revokeInvitation`):**
    1.  **Authorization:** Verifies the `inviterId` has permission to revoke (original inviter or organization creator).
    2.  **Status Check:** Only `PENDING` invitations can be revoked.
    3.  **Update Status:** Sets the invitation `status` to `REVOKED`.

--- 

### **5. Listing Invitations**

*   **Endpoint:** `GET /api/invitations`
*   **Authentication:** Requires authentication. The authenticated user must have the `HIRING_MANAGER` role.
*   **Request Type:** `GET`
*   **Query Parameters:** Standard pagination parameters (`page`, `size`, `sort`).
*   **Response:** `Page<RecruiterInvitationDto>`
*   **Backend Logic (`InvitationServiceImpl.getOrganizationInvitations`):**
    1.  **Authorization:** Verifies the user belongs to an organization.
    2.  **Retrieve Invitations:** Fetches paginated `RecruiterInvitation` entities for the user's organization.

--- 

### Important Notes for Frontend Developers:

*   **Token Handling:** The invitation token is a UUID and should be treated as sensitive. It's used for one-time registration/acceptance.
*   **Redirection:** After successful invitation acceptance, the backend returns a JWT. The frontend should store this JWT and redirect the user to the appropriate authenticated section of the application.
*   **Error Handling:** Be prepared to handle `400 Bad Request` for invalid/expired tokens, `403 Forbidden` for unauthorized actions, and `404 Not Found` for non-existent resources.
*   **Invitation Expiry:** Invitations have an expiry time (default 72 hours). The frontend should display this information from the `validate` endpoint and handle expired invitations gracefully.
