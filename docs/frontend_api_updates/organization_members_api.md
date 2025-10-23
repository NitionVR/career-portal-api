# Frontend API Update: Organization Members Management

This document outlines the new API endpoint for fetching organization members.

## 1. Organization Members List

### `GET /api/organization/members`

*   **Description:** Fetches a list of all members (Hiring Managers and Recruiters) belonging to the current user's organization.
*   **Authentication:** Requires authentication. The authenticated user must have either the `HIRING_MANAGER` or `RECRUITER` role.
*   **Request Type:** `GET`
*   **Endpoint URL:** `/api/organization/members`
*   **Query Parameters:** None.

*   **Response:** `List<OrganizationMemberDto>`

    A list of `OrganizationMemberDto` objects, representing the members of the organization.

*   **`OrganizationMemberDto` Structure:**

    ```typescript
    interface OrganizationMemberDto {
      id: string; // UUID of the User
      email: string;
      firstName: string;
      lastName: string;
      role: 'HIRING_MANAGER' | 'RECRUITER';
      status: 'ACTIVE' | 'DISABLED'; // Currently always 'ACTIVE' as soft-delete is not yet implemented for members
      joinedDate: string; // ISO 8601 format (LocalDateTime from backend)
    }
    ```

### Important Notes for Frontend Developers:

*   **Authentication:** Ensure a valid JWT token for a `HIRING_MANAGER` or `RECRUITER` is included in the `Authorization` header.
*   **Role-Based Access:** This endpoint is only accessible by Hiring Managers and Recruiters within the same organization.
*   **Member Status:** The `status` field in `OrganizationMemberDto` currently always returns `ACTIVE`. Future enhancements may introduce `DISABLED` status for soft-deleted members.
