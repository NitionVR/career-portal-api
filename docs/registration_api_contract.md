# API Contract Documentation: Registration Feature

This document outlines the API endpoints, request/response formats, and error handling for the user registration feature, catering to both Candidate and Hiring Manager roles.

## Base URL

`/api/auth`

## 1. Initiate Registration (Common for both roles)

This endpoint initiates the registration process by sending a magic link to the provided email address.

### `POST /api/auth/register`

**Description:** Initiates the registration process for a new user by sending a magic link to their email. The magic link will contain a token that can be used to complete the registration.

**Request:**

*   **Method:** `POST`
*   **Content-Type:** `application/json`
*   **Body:** `RegistrationRequest` DTO
    ```json
    {
      "email": "user@example.com",
      "role": "CANDIDATE" | "HIRING_MANAGER"
    }
    ```
    *   `email` (string, required): The user's email address. Must be a valid email format.
    *   `role` (string, required): The desired role for the user. Must be either "CANDIDATE" or "HIRING_MANAGER".

**Response:**

*   **Status:** `200 OK`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "message": "Registration link sent to your email"
    }
    ```

**Error Responses:**

*   `400 Bad Request` - `MethodArgumentNotValidException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 400,
          "error": "Validation Failed",
          "message": {
            "email": "must be a well-formed email address",
            "role": "must not be null"
          }
        }
        ```
*   `400 Bad Request` - `BadRequestException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 400,
          "error": "Bad Request",
          "message": "User with this email already exists"
        }
        ```

---

## 2. Validate Registration Token

This endpoint allows the frontend to validate the registration token received via email before rendering the completion form.

### `GET /api/auth/validate-registration-token`

**Description:** Validates a registration token and returns information about the user and their intended role.

**Request:**

*   **Method:** `GET`
*   **Query Parameter:**
    *   `token` (string, required): The JWT token received in the magic link.

**Response:**

*   **Status:** `200 OK`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "role": "CANDIDATE" | "HIRING_MANAGER",
      "valid": true,
      "email": "user@example.com"
    }
    ```
    *   `role` (string): The role extracted from the token.
    *   `valid` (boolean): `true` if the token is valid and not expired, `false` otherwise.
    *   `email` (string): The email extracted from the token.

**Error Responses:**

*   `400 Bad Request` - `BadRequestException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 400,
          "error": "Bad Request",
          "message": "Invalid or expired token"
        }
        ```

---

## 3. Complete Candidate Registration

This endpoint completes the registration for a candidate user after their email has been verified via the magic link.

### `POST /api/auth/register/candidate`

**Description:** Completes the registration for a candidate user, creating their profile and issuing a session JWT.

**Request:**

*   **Method:** `POST`
*   **Query Parameter:**
    *   `token` (string, required): The registration token received in the magic link.
*   **Content-Type:** `application/json`
*   **Body:** `CandidateRegistrationDto` DTO
    ```json
    {
      "username": "john.doe",
      "firstName": "John",
      "lastName": "Doe",
      "gender": "Male",
      "race": "Caucasian",
      "disability": "None",
      "contactNumber": "+1234567890",
      "alternateContactNumber": "+1987654321"
    }
    ```
    *   `username` (string, required): Unique username for the candidate.
    *   `firstName` (string, required): Candidate's first name.
    *   `lastName` (string, required): Candidate's last name.
    *   `gender` (string, optional): Candidate's gender.
    *   `race` (string, optional): Candidate's race.
    *   `disability` (string, optional): Candidate's disability status.
    *   `contactNumber` (string, required): Candidate's contact number (international format, e.g., `+1234567890`).
    *   `alternateContactNumber` (string, optional): Alternate contact number (international format).

**Response:**

*   **Status:** `200 OK`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // Session JWT
    }
    ```
    *   `token` (string): A JWT token for the newly registered and logged-in user.

**Error Responses:**

*   `400 Bad Request` - `MethodArgumentNotValidException`
    *   **Body:** `ErrorResponse` (similar to initiate registration, with specific field errors)
*   `400 Bad Request` - `BadRequestException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 400,
          "error": "Bad Request",
          "message": "Invalid registration token for candidate"
        }
        ```
*   `400 Bad Request` - `DataIntegrityViolationException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 400,
          "error": "Bad Request",
          "message": "This username is already taken"
        }
        ```

---

## 4. Complete Hiring Manager Registration

This endpoint completes the registration for a Hiring Manager user after their email has been verified via the magic link.

### `POST /api/auth/register/hiring-manager`

**Description:** Completes the registration for a Hiring Manager user, creating their organization, profile, and issuing a session JWT.

**Request:**

*   **Method:** `POST`
*   **Query Parameter:**
    *   `token` (string, required): The registration token received in the magic link.
*   **Content-Type:** `application/json`
*   **Body:** `HiringManagerRegistrationDto` DTO
    ```json
    {
      "username": "jane.smith",
      "companyName": "Acme Corp",
      "industry": "Technology",
      "contactPerson": "Jane Smith",
      "contactNumber": "+1123456789"
    }
    ```
    *   `username` (string, required): Unique username for the hiring manager.
    *   `companyName` (string, required): Name of the hiring manager's company.
    *   `industry` (string, required): Industry of the company.
    *   `contactPerson` (string, required): Name of the contact person at the company.
    *   `contactNumber` (string, required): Company's contact number (international format, e.g., `+1123456789`).

**Response:**

*   **Status:** `200 OK`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // Session JWT
    }
    ```
    *   `token` (string): A JWT token for the newly registered and logged-in user.

**Error Responses:**

*   `400 Bad Request` - `MethodArgumentNotValidException`
    *   **Body:** `ErrorResponse` (similar to initiate registration, with specific field errors)
*   `400 Bad Request` - `BadRequestException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 400,
          "error": "Bad Request",
          "message": "Invalid registration token for hiring manager"
        }
        ```
*   `400 Bad Request` - `DataIntegrityViolationException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 400,
          "error": "Bad Request",
          "message": "This username is already taken"
        }
        ```

---

## 5. Verify Session (Common for all authenticated users)

This endpoint verifies the validity of a session JWT and issues a new one if valid.

### `GET /api/auth/verify`

**Description:** Verifies the validity of a session JWT. If valid, it issues a new session JWT.

**Request:**

*   **Method:** `GET`
*   **Query Parameter:**
    *   `token` (string, required): The session JWT to verify.

**Response:**

*   **Status:** `200 OK`
*   **Content-Type:** `application/json`
*   **Body:**
    ```json
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." // New session JWT
    }
    ```
    *   `token` (string): A new JWT token for the authenticated user.

**Error Responses:**

*   `400 Bad Request` - `BadRequestException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 400,
          "error": "Bad Request",
          "message": "Invalid or expired token"
        }
        ```
*   `401 Unauthorized` - `UnauthorizedException`
    *   **Body:** `ErrorResponse`
        ```json
        {
          "timestamp": "2025-10-07T12:00:00.000000",
          "status": 401,
          "error": "Unauthorized",
          "message": "User not authenticated"
        }
        ```