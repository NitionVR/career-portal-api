# Frontend API Update: Candidate Resume Management

This document outlines the new API endpoints for candidates to manage their resumes, including uploading, linking to their profile, deleting, and listing.

## 1. Resume Upload Flow

The resume upload process involves two main steps:
1.  Requesting a pre-signed URL from the backend to securely upload the file directly to S3.
2.  Notifying the backend after a successful S3 upload to link the resume URL to the candidate's profile.

### `POST /api/profile/me/resumes/upload-url`

*   **Description:** Requests a pre-signed S3 URL for a candidate to upload their resume directly to AWS S3. This endpoint performs initial validation (e.g., file type, size, max resume limit).
*   **Authentication:** Requires authentication. The authenticated user must have the `CANDIDATE` role.
*   **Request Type:** `POST`
*   **Endpoint URL:** `/api/profile/me/resumes/upload-url`
*   **Request Body:** `ResumeUploadRequest`

    ```typescript
    interface ResumeUploadRequest {
      contentType: string; // MIME type of the file (e.g., 'application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document')
      contentLength: number; // Size of the file in bytes
      fileName: string; // Original name of the file
    }
    ```

*   **Response:** `UploadUrlResponse`

    ```typescript
    interface UploadUrlResponse {
      uploadUrl: string; // The pre-signed URL to which the file should be PUT
      fileUrl: string;   // The public URL of the file after successful upload
      key: string;       // The S3 object key (path) of the uploaded file
    }
    ```

### `POST /api/profile/me/resumes`

*   **Description:** Links a successfully uploaded resume (via a pre-signed URL) to the candidate's profile. This must be called *after* the file has been successfully `PUT` to the `uploadUrl` obtained from the previous step.
*   **Authentication:** Requires authentication. The authenticated user must have the `CANDIDATE` role.
*   **Request Type:** `POST`
*   **Endpoint URL:** `/api/profile/me/resumes`
*   **Request Body:** `ResumeDto`

    ```typescript
    interface ResumeDto {
      id?: string;       // Optional: Backend will generate if not provided
      url: string;       // The public URL of the uploaded resume (from UploadUrlResponse.fileUrl)
      filename: string;  // The original filename of the resume
      uploadDate?: string; // Optional: Backend will set to current time
    }
    ```

*   **Response:** `ResumeDto` (the newly created resume entry, including its generated ID and upload date).

## 2. Resume Deletion

### `DELETE /api/profile/me/resumes/{resumeId}`

*   **Description:** Deletes a specific resume from the candidate's profile and removes the corresponding file from S3.
*   **Authentication:** Requires authentication. The authenticated user must have the `CANDIDATE` role.
*   **Request Type:** `DELETE`
*   **Endpoint URL:** `/api/profile/me/resumes/{resumeId}`
*   **Path Parameters:**
    *   `resumeId`: `string` (UUID) - The ID of the resume to delete.
*   **Response:** `204 No Content`

## 3. List Resumes

### `GET /api/profile/me/resumes`

*   **Description:** Retrieves a list of all resumes associated with the authenticated candidate's profile.
*   **Authentication:** Requires authentication. The authenticated user must have the `CANDIDATE` role.
*   **Request Type:** `GET`
*   **Endpoint URL:** `/api/profile/me/resumes`
*   **Response:** `List<ResumeDto>`

## Important Notes for Frontend Developers:

*   **Authentication:** Ensure a valid JWT token for a `CANDIDATE` is included in the `Authorization` header for all resume-related operations.
*   **Upload Flow:**
    1.  Call `POST /api/profile/me/resumes/upload-url` with `contentType`, `contentLength`, and `fileName`.
    2.  Receive `uploadUrl` and `fileUrl` from the response.
    3.  Perform an HTTP `PUT` request directly to the `uploadUrl` with the resume file as the request body. Set the `Content-Type` header to match the `contentType` used in step 1. **Do NOT include `Content-Length` or `x-amz-meta-*` headers in the PUT request.**
    4.  After the `PUT` to S3 is successful, call `POST /api/profile/me/resumes` with a `ResumeDto` containing the `fileUrl` and `filename`.
*   **Max Resumes:** A candidate can upload a maximum of 3 resumes. The backend will enforce this limit and return a `400 Bad Request` if exceeded.
*   **File Types:** Supported content types for resumes are typically `application/pdf` and `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (for .docx). The backend validates this.
*   **AI Extraction:** The AI extraction functionality is a separate backend process that will consume the uploaded resume. The frontend does not need to directly interact with the AI endpoint for the upload process itself. Further documentation will be provided for AI integration.
