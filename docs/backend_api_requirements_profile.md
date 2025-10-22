# Backend API Requirements: Candidate Profile Management

This document outlines the API endpoints required to enable candidates to build, manage, and automatically populate their professional profiles.

---

### **P0: Critical - Full Profile CRUD Endpoints**

**Objective:** To provide the core functionality for a user to create, read, and update their entire professional profile with a single, atomic operation. The data structure should align with the widely-used **JSON Resume schema**.

#### **1. Get Full Profile**

*   **Endpoint:** `GET /api/profile/me`
*   **Description:** Retrieves the current authenticated user's complete profile object.
*   **Success Response (200 OK):**
    *   A single JSON object representing the user's entire profile.
    *   If the user has not created a profile yet, it should return a `200 OK` with a default or empty structure.
*   **Example Response Payload:**
    ```json
    {
      "basics": {
        "name": "John Doe",
        "label": "Software Engineer",
        "email": "john@example.com",
        "phone": "+14155552671",
        "url": "https://johndoe.com",
        "summary": "A summary of John Doe...",
        "location": {
          "address": "123 Main St",
          "postalCode": "94107",
          "city": "San Francisco",
          "countryCode": "US",
          "region": "California"
        }
      },
      "work": [
        {
          "name": "Company A",
          "position": "Senior Developer",
          "startDate": "2022-01-01",
          "endDate": "2023-12-31",
          "summary": "Description of my role..."
        }
      ],
      "education": [],
      "skills": [],
      "languages": [],
      "interests": [],
      "references": [],
      "projects": []
      // ... and all other sections
    }
    ```

#### **2. Create / Update Full Profile**

*   **Endpoint:** `PUT /api/profile/me`
*   **Description:** Creates or completely replaces the authenticated user's profile with the provided data. This is a full "upsert" operation.
*   **Request Body:**
    *   A single JSON object with the exact same structure as the `GET /api/profile/me` response.
*   **Success Response (200 OK):** The newly saved, complete profile object.
*   **Impact:** This is the most critical endpoint. It allows the user to save all their changes from the profile form in a single, atomic transaction.

---

