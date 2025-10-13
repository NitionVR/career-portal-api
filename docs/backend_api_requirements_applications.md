# Backend API Requirements: My Applications

This document outlines the new API endpoints required to support the "My Applications" feature for authenticated talent users.

## Data Models

### ApplicationSummaryDto

Used when listing multiple applications.

```json
{
  "id": "string",
  "applicationDate": "string (date-time)",
  "status": "string (e.g., 'Under Review', 'Interview Scheduled')",
  "job": {
    "id": "string",
    "title": "string",
    "company": "string"
  }
}
```

### ApplicationDetailsDto

Used when fetching a single, detailed application view.

```json
{
  "id": "string",
  "applicationDate": "string (date-time)",
  "status": "string",
  "job": { ...JobPostResponse }, // The full job post object
  "communicationHistory": [
    {
      "status": "string",
      "date": "string (date-time)",
      "message": "string (optional)"
    }
  ]
}
```

---

## New Endpoints

### 1. List My Applications

- **Description:** Retrieves a paginated list of all jobs the currently authenticated user has applied for.
- **Endpoint:** `GET /api/applications/me`
- **Security:** Requires an authenticated user with the 'Talent' role.
- **Query Parameters:**
    - `pageable`: Standard Spring pagination object (`page`, `size`).
    - `search` (optional, string): A search term to match against the job title or company name.
    - `sort` (optional, string): The field to sort by. Should support `date` (applicationDate), `company` (job.company), and `status`.
- **Success Response:**
    - **Code:** `200 OK`
    - **Body:** A paginated response object containing a list of `ApplicationSummaryDto`.
      ```json
      {
        "content": [ ...ApplicationSummaryDto ],
        "totalPages": "number",
        "totalElements": "number",
        "number": "number (current page)",
        // ... other pagination fields
      }
      ```

### 2. Get Application Details

- **Description:** Retrieves the full details for a single application, including the full job description and communication history.
- **Endpoint:** `GET /api/applications/{id}`
- **Security:** Requires the authenticated user to be the owner of the application.
- **Path Parameters:**
    - `id`: The ID of the application to retrieve.
- **Success Response:**
    - **Code:** `200 OK`
    - **Body:** An `ApplicationDetailsDto` object.

### 3. Apply for a Job

- **Description:** Allows a user to apply for a specific job.
- **Endpoint:** `POST /api/job-posts/{id}/apply`
- **Security:** Requires an authenticated user with the 'Talent' role.
- **Path Parameters:**
    - `id`: The ID of the job post to apply for.
- **Request Body:** (Optional) Can be empty or contain a cover letter, resume reference, etc., if needed in the future.
- **Success Response:**
    - **Code:** `201 Created`
    - **Body:** The newly created `ApplicationSummaryDto` object.

### 4. Withdraw Application

- **Description:** Allows a user to withdraw their application for a job.
- **Endpoint:** `DELETE /api/applications/{id}`
- **Security:** Requires the authenticated user to be the owner of the application.
- **Path Parameters:**
    - `id`: The ID of the application to withdraw.
- **Success Response:**
    - **Code:** `204 No Content`
