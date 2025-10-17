# Production-Ready Authentication Flow

This document outlines the robust, passwordless magic link authentication flow for the eTalente application. It details the backend API endpoints, the JWT structure, and the expected frontend interaction.

## Authentication Overview

The authentication process is entirely passwordless and relies on time-sensitive magic links sent to a user's email.

1.  **Request Magic Link**: The user enters their email address in the frontend application. The frontend sends this email to the backend's `/api/auth/login` endpoint.
2.  **Send Magic Link**: The backend generates a secure, single-use One-Time Token (OTT), stores it, and emails a magic link containing the OTT to the user.
3.  **Verify Token**: The user clicks the magic link, which directs them back to the frontend. The frontend captures the OTT from the URL and sends it to the backend's `/api/auth/verify` endpoint.
4.  **Issue Session Token**: The backend validates the OTT. If valid, it generates a JSON Web Token (JWT) that acts as the user's session token and returns it to the frontend, along with user details.
5.  **Authenticated Session**: The frontend stores the JWT securely (in `localStorage`) and includes it in the `Authorization: Bearer <token>` header for all subsequent requests to protected API endpoints.

---

## API Endpoints

### 1. Request Magic Link

-   **Endpoint**: `POST /api/auth/login`
-   **Description**: Initiates the login process by sending a magic link to a user's email. This endpoint is for **existing users only**. If the email does not exist in the database, a `404 Not Found` error is returned.
-   **Request Body**:
    ```json
    {
      "email": "user@example.com"
    }
    ```
-   **Success Response (200 OK)**:
    ```json
    {
      "message": "Magic link sent to your email",
      "email": "user@example.com"
    }
    ```
-   **Error Response (404 Not Found)**:
    ```json
    {
        "timestamp": "2025-10-17T19:50:03.759564",
        "status": 404,
        "error": "Not Found",
        "message": "User with email nonexistent@example.com not found."
    }
    ```

### 2. Verify Magic Link Token (OTT)

-   **Endpoint**: `GET /api/auth/verify`
-   **Description**: Verifies the One-Time Token (OTT) from the magic link and exchanges it for a session JWT.
-   **Query Parameter**: `token` (The OTT from the magic link URL)
-   **Example URL**: `https://api.etalente.app/api/auth/verify?token=a1b2c3d4-e5f6-7890-1234-567890abcdef`
-   **Success Response (200 OK)**:
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "id": "c4a8c1a0-5b5a-4b1a-8f0a-1a2b3c4d5e6f",
        "email": "user@example.com",
        "role": "CANDIDATE",
        "firstName": "John",
        "lastName": "Doe",
        "isNewUser": false
      },
      "expiresIn": 3600000
    }
    ```

### 3. Check Session Status

-   **Endpoint**: `GET /api/auth/session`
-   **Description**: Allows the frontend to check if the current user's JWT is still valid. This is useful on application startup to determine if the user should be automatically logged in.
-   **Headers**: `Authorization: Bearer <jwt>`
-   **Authenticated Response**:
    ```json
    {
      "authenticated": true,
      "userId": "c4a8c1a0-5b5a-4b1a-8f0a-1a2b3c4d5e6f",
      "email": "user@example.com",
      "role": "CANDIDATE",
      "isNewUser": false,
      "expiresIn": 3456000
    }
    ```
-   **Unauthenticated Response**:
    ```json
    {
      "authenticated": false
    }
    ```

### 4. Refresh Session Token

-   **Endpoint**: `POST /api/auth/refresh`
-   **Description**: Generates a new JWT with a renewed expiration time. This allows the frontend to maintain a user's session without requiring them to log in again.
-   **Headers**: `Authorization: Bearer <jwt>`
-   **Response**:
    ```json
    {
      "token": "new_eyJhbGciOiJIUzI1NiJ9...",
      "expiresIn": 3600000
    }
    ```

### 5. Logout

-   **Endpoint**: `POST /api/auth/logout`
-   **Description**: A stateless endpoint to signify logout. In a JWT-based system, logout is primarily handled client-side by deleting the token. This endpoint can be used for server-side logging or future stateful operations (e.g., token blacklisting).
-   **Response**:
    ```json
    {
      "message": "Logged out successfully"
    }
    ```

---

## JSON Web Token (JWT) Structure

The session token is a JWT containing the following claims in its payload:

-   **`sub` (Subject)**: The unique identifier (`UUID`) of the user.
-   **`email`**: The user's email address.
-   **`role`**: The user's role (e.g., `CANDIDATE`, `RECRUITER`, `HIRING_MANAGER`).
-   **`is_new_user`**: A boolean flag indicating if this is the user's first session.
-   **`iat` (Issued At)**: The timestamp when the token was issued.
-   **`exp` (Expiration Time)**: The timestamp when the token will expire.

This structure allows the backend to perform authorization checks directly from the token without needing a database query for user details on every request, significantly improving performance.
