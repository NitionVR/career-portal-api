# Frontend API Update: Bulk Actions and Export

This document outlines the new API endpoints for performing bulk actions on applicants and exporting applicant data.

## 1. Bulk Status Update

### `POST /api/applicants/bulk-update-status`

*   **Description:** Updates the status of multiple job applications at once.
*   **Authentication:** Requires authentication. The authenticated user must have either the `HIRING_MANAGER` or `RECRUITER` role.
*   **Request Type:** `POST`
*   **Endpoint URL:** `/api/applicants/bulk-update-status`
*   **Request Body:** `BulkStatusUpdateRequest`

    ```typescript
    interface BulkStatusUpdateRequest {
      applicationIds: string[]; // Array of JobApplication UUIDs
      targetStatus: string; // e.g., 'APPLIED', 'UNDER_REVIEW', 'INTERVIEW_SCHEDULED', 'OFFER_EXTENDED', 'HIRED', 'REJECTED', 'WITHDRAWN'
      sendNotification?: boolean; // Optional, default: false
      note?: string; // Optional note about the status change
    }
    ```

*   **Response:** `BulkActionResponse`

    ```typescript
    interface BulkActionResponse {
      totalRequested: number;
      successCount: number;
      failureCount: number;
      errors: BulkActionError[];
      processedAt: string; // ISO 8601 format
    }

    interface BulkActionError {
      applicationId: string; // UUID of the failed application
      error: string; // e.g., 'UNAUTHORIZED', 'INVALID_TRANSITION', 'PROCESSING_ERROR'
      reason: string; // Detailed error message
    }
    ```

## 2. Export Applicants

### `GET /api/applicants/export`

*   **Description:** Exports a list of applicants to a file (CSV, Excel, or PDF).
*   **Authentication:** Requires authentication. The authenticated user must have either the `HIRING_MANAGER` or `RECRUITER` role.
*   **Request Type:** `GET`
*   **Endpoint URL:** `/api/applicants/export`
*   **Query Parameters:**
    *   `format`: `string` (Optional, default: `CSV`) - The desired export format. Can be `CSV`, `EXCEL`, or `PDF`.
    *   All filtering parameters from `GET /api/applicants` are also supported here (e.g., `search`, `jobId`, `statuses`, etc.).

*   **Response:** A file download with the appropriate content type.

### Important Notes for Frontend Developers:

*   **Bulk Actions:** The `bulk-update-status` endpoint is designed to be partially successful. Always check the `failureCount` and `errors` array in the response to inform the user of any applications that failed to update.
*   **Export:** The `export` endpoint is a `GET` request. The browser will handle the file download. The frontend should construct the query parameters based on the user's selected filters.
