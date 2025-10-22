# Frontend API Update: Applicant Management Module

This document outlines the new API endpoint for fetching applicant data, implemented as part of the employer-facing features.

## 1. Applicant Management Module (`/api/applicants`)

### `GET /api/applicants`

*   **Description:** Fetches a paginated and filterable list of applicants for the current employer's organization. This endpoint is designed for Hiring Managers and Recruiters to view and manage job applications.

*   **Authentication:** Requires authentication. The authenticated user must have either the `HIRING_MANAGER` or `RECRUITER` role.

*   **Request Type:** `GET`

*   **Endpoint URL:** `/api/applicants`

*   **Query Parameters:**
    *   `page`: `number` (Optional, default: 0) - The page number to retrieve (0-indexed).
    *   `size`: `number` (Optional, default: 20) - The number of items per page.
    *   `sort`: `string` (Optional, default: `applicationDate,desc`) - Sorting criteria in the format `property,direction` (e.g., `candidateName,asc`, `applicationDate,desc`).
    *   `search`: `string` (Optional) - General text search term applied to candidate's first name, last name, and job title.
    *   `jobId`: `string` (Optional) - Filters applicants by a specific job post ID.
    *   `status`: `string[]` (Optional) - Filters applicants by one or more job application statuses (e.g., `APPLIED`, `UNDER_REVIEW`, `INTERVIEW_SCHEDULED`).
    *   `skills`: `string[]` (Optional) - Filters applicants whose associated job post skills (JSONB field) contain the specified skill(s). (Note: This performs a broad text search within the JSONB string representation of skills).
    *   `experienceMin`: `number` (Optional) - Filters applicants with a minimum number of years of experience. (Searches within the `profile` JSONB field).
    *   `education`: `string[]` (Optional) - Filters applicants whose education details (within the `profile` JSONB field) contain the specified education term(s). (Note: This performs a broad text search within the JSONB string representation of education).
    *   `location`: `string` (Optional) - Filters applicants by the location specified in the job post (within the `location` JSONB field, specifically the `city`). (Note: This performs a broad text search within the JSONB string representation of location).
    *   `aiMatchScoreMin`: `number` (Optional) - Filters applicants with a minimum AI match score (0-100). (Currently not implemented on the backend, will be ignored).

*   **Response:** `Page<ApplicantSummaryDto>`

    A standard Spring Data `Page` object containing a list of `ApplicantSummaryDto` objects, along with pagination metadata (e.g., `totalPages`, `totalElements`, `number`, `size`, `first`, `last`).

*   **`ApplicantSummaryDto` Structure:**

    ```typescript
    interface ApplicantSummaryDto {
      id: string; // UUID of the JobApplication
      candidateName: string; // Combination of candidate's firstName and lastName
      jobTitle: string; // Title of the JobPost
      jobId: string; // UUID of the JobPost
      profileImageUrl?: string; // URL to the candidate's profile image
      aiMatchScore: number; // Placeholder, currently null from backend
      skills: string[]; // List of skill names extracted from JobPost skills JSONB
      experienceYears: number; // Extracted from candidate's profile JSONB
      location: string; // Extracted from JobPost location JSONB (city)
      applicationDate: string; // ISO 8601 format (LocalDateTime from backend)
      status: string; // Current status of the JobApplication (e.g., 'APPLIED', 'UNDER_REVIEW')
    }
    ```

### Important Notes for Frontend Developers:

*   **Authentication:** Ensure a valid JWT token for a `HIRING_MANAGER` or `RECRUITER` is included in the `Authorization` header.
*   **Pagination:** The response is a `Page` object. Extract the `content` array for the list of applicants and use the pagination metadata to build UI controls.
*   **Filtering:** Construct query parameters dynamically based on user input. For array parameters like `status`, `skills`, `education`, pass them as multiple `&status=APPLIED&status=UNDER_REVIEW` or comma-separated if your client library handles it.
*   **JSONB Filtering Limitations:** The backend's JSONB filtering for `skills`, `education`, and `location` currently performs a broad text search within the string representation of the JSONB field. This is a simplified approach. For more precise filtering (e.g., exact match within a JSONB array element), further backend enhancements would be required.
*   **`aiMatchScore`:** This field is currently a placeholder and will always be `null` from the backend. It should be treated as optional and not relied upon for filtering or display until fully implemented on the backend.
