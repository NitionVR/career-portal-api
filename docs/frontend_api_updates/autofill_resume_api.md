# Frontend API Update: Candidate Profile Autofill from Resume

This document outlines the new API endpoint for candidates to automatically autofill their profile information by extracting data from an uploaded resume.

## 1. Profile Autofill from Resume

### `POST /api/profile/me/autofill-resume`

*   **Description:** Triggers the backend to extract structured data from a previously uploaded resume (identified by its S3 URL) and automatically merge this data into the candidate's profile. This helps candidates quickly populate their profile without manual entry.
*   **Authentication:** Requires authentication. The authenticated user must have the `CANDIDATE` role.
*   **Request Type:** `POST`
*   **Endpoint URL:** `/api/profile/me/autofill-resume`
*   **Request Body:** `ResumeDto`

    ```typescript
    interface ResumeDto {
      url: string; // The S3 URL of the uploaded resume document
      // Other fields like id, filename, uploadDate are not required for this request
    }
    ```

*   **Response:** `JsonNode`

    The updated `profile` JSONB object of the user, reflecting the merged data from the resume. This will contain various fields extracted from the resume (e.g., education, experience, contact information).

### Important Notes for Frontend Developers:

*   **Authentication:** Ensure a valid JWT token for a `CANDIDATE` is included in the `Authorization` header.
*   **Prerequisite:** The resume document must first be uploaded to S3 using the resume upload flow (`POST /api/profile/me/resumes/upload-url` followed by `PUT` to S3 and `POST /api/profile/me/resumes`). The `url` provided in the request body for this endpoint should be the `fileUrl` obtained from the initial `UploadUrlResponse`.
*   **Autofill Logic:**
    *   The backend calls an external AI document parser service to extract data from the provided resume URL.
    *   The extracted data is then merged into the candidate's existing `profile` JSONB field.
    *   **Important:** The `firstName` and `lastName` fields are explicitly **NOT** autofilled by this process. These fields must be managed separately by the user.
    *   After successful autofill, the user's `profileComplete` flag is automatically set to `true`.
*   **Automatic Save:** The updated profile data is automatically saved to the database by the backend.
*   **Error Handling:** Be prepared to handle `400 Bad Request` for invalid resume URLs or if the user is not a candidate, and `500 Internal Server Error` if the external document parser service fails or returns invalid data.
*   **Response Data:** The response contains the full updated `profile` JSONB. The frontend should use this to update the UI accordingly.
