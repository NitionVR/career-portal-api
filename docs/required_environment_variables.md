# Required Environment Variables for Etalente Backend

This document lists all environment variables that need to be set for the Etalente backend application to run successfully, both locally with the `prod` profile and when deployed to AWS.

## 1. Database Configuration

*   **`SPRING_DATASOURCE_URL`**: JDBC URL for the PostgreSQL database.
    *   _Example (Local):_ `jdbc:postgresql://localhost:5432/etalente`
    *   _Example (AWS RDS):_ `jdbc:postgresql://your-rds-endpoint:5432/etalente`
*   **`SPRING_DATASOURCE_USERNAME`**: Username for the database connection.
    *   _Example (Local):_ `user`
    *   _Example (AWS RDS):_ `postgres` (or your chosen master username)
*   **`SPRING_DATASOURCE_PASSWORD`**: Password for the database connection.
    *   _Example (Local):_ `password`
    *   _Example (AWS RDS):_ Your secure master password

## 2. SMTP (Simple Mail Transfer Protocol) Configuration (for Google SMTP)

*   **`SPRING_MAIL_HOST`**: SMTP server host. For Gmail, this is `smtp.gmail.com`.
*   **`SPRING_MAIL_PORT`**: SMTP server port. For Gmail, this is typically `587` (for TLS) or `465` (for SSL).
*   **`SPRING_MAIL_USERNAME`**: Your full Gmail address (e.g., `your-email@gmail.com`).
*   **`SPRING_MAIL_PASSWORD`**: Your Google App Password (recommended) or Gmail password (if less secure app access is enabled). **Critical for security.**
*   **`SPRING_MAIL_FROM`**: The email address from which emails will be sent (e.g., `no-reply@your-domain.com`).

## 3. JWT (JSON Web Token) Configuration

*   **`JWT_SECRET`**: A strong, unique secret key used for signing and verifying JWTs. **Critical for security.**
    *   _Example:_ `your_super_secret_jwt_key_that_is_at_least_32_characters_long`
*   **`JWT_EXPIRATION_MS`**: (Optional) Expiration time for JWTs in milliseconds. Defaults to 1 hour (3600000).
    *   _Example:_ `3600000`

## 3. Application URLs

*   **`MAGIC_LINK_URL`**: The base URL for magic links sent via email. This should point to your frontend application's authentication callback.
    *   _Example:_ `https://your-frontend-domain.com/auth/callback`
*   **`INVITATION_LINK_URL`**: The base URL for invitation links sent via email. This should point to your frontend application's invitation acceptance page.
    *   _Example:_ `https://your-frontend-domain.com/auth/accept-invitation`

## 4. CORS (Cross-Origin Resource Sharing) Configuration

*   **`CORS_ALLOWED_ORIGINS`**: A comma-separated list of origins that are allowed to access the backend API. **Critical for security in production.**
    *   _Example:_ `https://your-frontend-domain.com,https://www.your-frontend-domain.com`

## 5. Novu Notification Service Configuration

*   **`NOVU_API_KEY`**: Your API key for the Novu notification service. **Critical for notifications.**
    *   _Example:_ `sk_your_novu_api_key`
*   **`NOVU_BACKEND_URL`**: (Optional) The base URL for the Novu API. Defaults to `https://api.novu.co/v1/`.
    *   _Example:_ `https://api.novu.co/v1/`

## 6. AWS S3 Configuration (for Uploads)

*   **`AWS_REGION`**: The AWS region where your S3 bucket is located. This is used for S3 client configuration. (Note: AWS credentials are typically handled via IAM roles in deployed environments, not explicit keys in application config).
    *   _Example:_ `af-south-1`

## 7. Upload Configuration

*   **`UPLOAD_MAX_FILE_SIZE`**: (Optional) Maximum allowed file size for uploads in bytes. Defaults to 5MB (5242880).
    *   _Example:_ `5242880`
*   **`UPLOAD_ALLOWED_CONTENT_TYPES`**: (Optional) Comma-separated list of allowed content types for uploads. Defaults to `image/jpeg,image/png`.
    *   _Example:_ `image/jpeg,image/png,application/pdf`
*   **`PRESIGNED_URL_EXPIRATION_MINUTES`**: (Optional) Expiration time for S3 presigned URLs in minutes. Defaults to 15 minutes.
    *   _Example:_ `15`

## 8. Other Optional Configurations

*   **`INVITATION_EXPIRY_HOURS`**: (Optional) Expiry time for invitations in hours. Defaults to 72 hours.
*   **`OTT_EXPIRATION_MINUTES`**: (Optional) Expiration time for One-Time Tokens in minutes. Defaults to 15 minutes.

## 9. Local Profiles for Email Sending

To easily switch between Maildev and Google SMTP for local testing:

*   **Maildev (default local profile):** No special profile needed. Just run the application with the `local` profile (e.g., `spring.profiles.active=local`).
*   **Google SMTP (`google-smtp-local` profile):** To use Google SMTP for local testing, activate the `google-smtp-local` profile (e.g., `spring.profiles.active=google-smtp-local`). When this profile is active, you will need to set the following environment variables:
    *   `SPRING_MAIL_USERNAME`
    *   `SPRING_MAIL_PASSWORD`
    *   `SPRING_MAIL_FROM`

---

**Note:** When deploying to AWS ECS, these environment variables are typically passed to the ECS tasks via the Task Definition. For sensitive variables like `DB_MASTER_PASSWORD`, `JWT_SECRET`, and `NOVU_API_KEY`, it is highly recommended to use AWS Secrets Manager and reference them in your CloudFormation templates or ECS Task Definitions.