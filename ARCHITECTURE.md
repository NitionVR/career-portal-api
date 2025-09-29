# Architecture & Communication Log

This document serves as the central point of communication and documentation for the `etalente-clone` (backend) and `etalente-clone-frontend` projects. Its purpose is to ensure that agents and developers working on either project have a clear, up-to-date understanding of the overall system architecture and API contract.

## Guiding Principles

1.  **Single Source of Truth:** This document is the primary reference for the API contract.
2.  **Document As You Develop:** Any change to an "API endpoint" (creation, modification, deletion) on the backend must be reflected here immediately. Any decision on the frontend on how to consume the API should also be logged.
3.  **Clarity and Brevity:** Use clear language. Provide examples where necessary.

---

## Backend (`etalente-clone`)

*   **Technology:** Java / Spring Boot
*   **Location:** `/Users/nition/et/etalente-clone`
*   **Description:** Provides the server-side logic and data persistence for the Etalente application. It will expose a RESTful API for the frontend to consume.

### API Contract

The following endpoints are implemented for the magic link authentication flow.

#### `POST /api/auth/login`

*   **Description:** Initiates the login or registration process by sending a magic link to the user's email.
*   **Request Body:**
    ```json
    {
      "email": "user@example.com"
    }
    ```
*   **Success Response (200 OK):**
    *   **Content-Type:** `text/plain`
    *   **Body:** `Magic link sent. Please check your email.`

#### `GET /api/auth/verify`

*   **Description:** Verifies the token from a magic link and, if valid, returns a JWT for session management.
*   **Request:**
    *   `token` (URL query parameter): The unique token sent to the user's email.
    *   Example: `/api/auth/verify?token=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
*   **Success Response (200 OK):**
    *   **Content-Type:** `text/plain`
    *   **Body:** A JWT string (e.g., `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`)

---

## Frontend (`etalente-clone-frontend`)

*   **Technology:** Angular
*   **Location:** `/Users/nition/et/etalente-clone-frontend`
*   **Description:** Provides the user interface for the Etalente application. It interacts with the backend via its RESTful API.

### API Consumption Notes

The frontend authentication flow is implemented as follows:

*   **`core/services/auth.ts`:** An `AuthService` handles all communication with the backend API. It provides methods for `login()` and `verify()`.
*   **`pages/login/login.ts`:** The `LoginPage` component uses the `AuthService` to call the `POST /api/auth/login` endpoint when the user submits their email.
*   **`pages/magic-link/magic-link.ts`:** The `MagicLinkPage` component handles the callback from the email link. It extracts the token from the URL and uses the `AuthService` to call `GET /api/auth/verify`, storing the returned JWT upon success.

---

## Project Roadmap Overview

The following is a high-level summary of the development plan, derived from `etalente-clone-frontend/project.spec`. It outlines the major features (Epics) planned for the application.

*   **Epic 1: Project Foundation & Setup:** Establish the core infrastructure, CI/CD pipelines, and foundational project structures for both the backend and frontend.
*   **Epic 2: User Management & Authentication:** Implement a passwordless (magic link) authentication system, user registration, profile creation, and role-based access control.
*   **Epic 3: Job Post Management:** Develop features for creating, managing, and publishing job posts, including a state machine for tracking their lifecycle.
*   **Epic 4: Application Management:** Build the functionality for candidates to apply for jobs and for recruiters to manage and track those applications.
*   **Epic 5: Testing & Quality Assurance:** Implement comprehensive unit, integration, and end-to-end tests for both backend and frontend to ensure quality.
*   **Epic 6: Performance & Scalability:** Optimize the application's performance through database indexing, caching, and load testing.
*   **Epic 7: Security & Compliance:** Harden the application against security vulnerabilities and ensure data protection compliance.
*   **Epic 8: Documentation & Deployment:** Create technical and user documentation and deploy the final application to the production environment.

