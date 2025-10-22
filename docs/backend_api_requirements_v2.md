# Backend API Requirements v2: Employer Features & Profile Enhancements

This document outlines the next phase of required backend API changes to support the Employer MVP features and enhance user profiles.

---

### **P0: Critical - Complete the Employer Dashboard**

**Objective:** To make the Employer Dashboard fully functional by providing all necessary data for the job post list.

**Endpoint to Modify:** `GET /api/job-posts/my-posts`

**Current State:** The endpoint returns a `Page<JobPostResponse>`, but the `JobPostResponse` objects are missing key fields required by the frontend UI.

**Required Changes:**

The `JobPostResponse` model **must** be updated to include the following three fields:

1.  **`applicantsCount`**
    *   **Type:** `number`
    *   **Description:** The total number of candidates who have applied to this specific job post.

2.  **`newApplicantsCount`**
    *   **Type:** `number`
    *   **Description:** The count of new applicants for a job post that the hiring manager has not yet viewed.

3.  **`companyLogoUrl`**
    *   **Type:** `string` (URL)
    *   **Description:** The public URL for the company's logo to be displayed on the job card.

**Example of Expected `JobPostResponse` Item:**
```json
{
  "id": "123",
  "title": "Software Engineer",
  "companyName": "Tech Solutions Inc.",
  "status": "PUBLISHED",
  "applicantsCount": 25,
  "newApplicantsCount": 3,
  "companyLogoUrl": "https://example.com/path/to/logo.png",
  // ... other existing properties
}
```

---

### **P1: High - Functional Profile Picture Uploads**

**Objective:** Allow both Candidates and Hiring Managers to upload and manage their profile pictures.

**Required New Endpoints:**

**1. Get Upload URL Endpoint**
*   **Suggested Endpoint:** `POST /api/profile/me/avatar/upload-url`
*   **Description:** To enable secure and direct-to-cloud storage uploads, the frontend first needs to request a short-lived, pre-signed URL from the backend.
*   **Request Body:**
    ```json
    {
      "contentType": "image/jpeg",
      "contentLength": 1048576 // File size in bytes
    }
    ```
*   **Success Response (200 OK):**
    ```json
    {
      "uploadUrl": "https://s3.amazonaws.com/your-bucket/...",
      "fileUrl": "https://cdn.your-domain.com/..." // The final URL of the file after upload
    }
    ```
*   **Security:** This endpoint must be authenticated.

**2. Update User Profile with Image URL Endpoint**
*   **Suggested Endpoint:** `PUT /api/profile/me/avatar`
*   **Description:** After the frontend successfully uploads the image to the cloud storage using the `uploadUrl`, it will call this endpoint to tell the backend to save the final `fileUrl` to the user's profile.
*   **Request Body:**
    ```json
    {
      "profileImageUrl": "https://cdn.your-domain.com/..."
    }
    ```
*   **Success Response (200 OK):** The updated `UserDto` object.
*   **Security:** This endpoint must be authenticated.

---

### **P2: Medium - Company Logo Uploads**

**Objective:** Allow Hiring Managers to upload and manage their company's logo.

**Required New Endpoints:**

This will follow the exact same secure two-step process as the user profile picture upload, but on the `Organization` entity.

**1. Get Company Logo Upload URL Endpoint**
*   **Suggested Endpoint:** `POST /api/organization/logo/upload-url`
*   **Description:** Request a pre-signed URL to upload the company logo.
*   **Request Body & Response:** Same structure as the profile picture endpoint.
*   **Security:** Authenticated and authorized (user must be a Hiring Manager of the organization).

**2. Update Organization with Logo URL Endpoint**
*   **Suggested Endpoint:** `PUT /api/organization/logo`
*   **Description:** Update the organization's profile with the final URL of the uploaded logo.
*   **Request Body:**
    ```json
    {
      "companyLogoUrl": "https://cdn.your-domain.com/..."
    }
    ```
*   **Success Response (200 OK):** The updated `OrganizationDto` object.
*   **Security:** Authenticated and authorized.
