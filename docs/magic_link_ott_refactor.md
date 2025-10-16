### Backend API Changes: One-Time Token (OTT) Authentication Flow

The magic link authentication flow has been refactored for enhanced security, moving from directly sending a JWT in the URL to using a One-Time Token (OTT) exchange mechanism.

**1. Removed Endpoint:**

*   **`GET /api/auth/verify?token={jwt_token}`**
    *   **Status:** **Removed.** This endpoint no longer exists on the backend. Any calls to it will result in a `404 Not Found` error.

**2. New Endpoint:**

*   **`POST /api/auth/exchange-ott`**
    *   **Description:** This endpoint is used to exchange a short-lived One-Time Token (OTT) for a long-lived session JWT. The frontend should call this endpoint after extracting the OTT from the magic link URL.
    *   **Request Method:** `POST`
    *   **Request Body:**
        *   The One-Time Token (OTT) as a plain string.
        *   **Example:**
            ```
            <ONE_TIME_TOKEN_STRING_FROM_URL>
            ```
            *(Note: The OTT is sent directly in the request body, not as a JSON object.)*
    *   **Success Response (200 OK):**
        *   **Content-Type:** `application/json`
        *   **Body:** A `RegistrationResponse` object containing the session JWT.
        *   **Example:**
            ```json
            {
              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiQ0FORElEQVRFIiwiaXNfbmV3X3VzZXIiOnRydWUsInN1YiI6InVzZXJAZXhhbXBsZS5jb20iLCJpYXQiOjE2NzgyMzU2NzgsImV4cCI6MTY3ODMyMjA3OH0.signature"
            }
            ```
        *   **JWT Claims:** The returned JWT will include the following claims:
            *   `role`: The user's role (e.g., `CANDIDATE`, `HIRING_MANAGER`).
            *   `is_new_user`: A boolean indicating if the user was newly created during this authentication flow.
    *   **Error Responses:**
        *   `400 Bad Request`: If the provided OTT is invalid, expired, or has already been used.

**3. Modified Magic Link URL Format:**

*   The URL sent in the magic link email to the user has changed.
*   **Old Format (Example):**
    `http://localhost:4200/auth/callback?token=eyJhbGciOiJIUzI1NiJ9...`
*   **New Format (Example):**
    `http://localhost:4200/auth/callback?ott=<ONE_TIME_TOKEN_HERE>`
    *   The frontend should now extract the `ott` query parameter from the URL.

---

**Clarification: Does the `verify` endpoint still work?**

No, that is correct. The `GET /api/auth/verify` endpoint has been removed from the backend. Any frontend code attempting to call this endpoint will now receive a `404 Not Found` error. The frontend should be updated to use the new `POST /api/auth/exchange-ott` endpoint for exchanging the One-Time Token.