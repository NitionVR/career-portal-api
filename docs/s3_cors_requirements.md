# S3 Bucket CORS Configuration Requirements

**Epic:** Complete File Upload Functionality

**User Story:** As a user, I want to be able to upload files (profile pictures, CVs, company logos) directly to S3 from my browser.

## Problem

The browser is receiving a `403 Forbidden` error when making a preflight `OPTIONS` request to the S3 pre-signed URL. This indicates that the S3 bucket's CORS (Cross-Origin Resource Sharing) policy is not configured to allow uploads from the frontend application's origin.

## Requirements

The S3 bucket (`etalente-uploads`) must be configured with a CORS policy that allows the following:

1.  **Allowed Origins:**
    *   `http://localhost:4200` (for local development)
    *   `http://etalente-alb-665506398.af-south-1.elb.amazonaws.com` (for the deployed production environment)

2.  **Allowed Methods:**
    *   `PUT` (to allow the file upload itself)
    *   `POST`
    *   `GET`

3.  **Allowed Headers:**
    *   `*` (Allow all headers, which is simplest for this use case) or be specific, e.g., `Authorization`, `x-amz-date`, `x-amz-content-sha256`, `content-type`, etc.

4.  **Expose Headers:**
    *   `ETag` (Often useful for verifying the integrity of the uploaded file)

## Example S3 CORS Configuration Rule

Here is an example of a CORS rule that should be applied to the `etalente-uploads` S3 bucket:

```json
[
    {
        "AllowedHeaders": [
            "*"
        ],
        "AllowedMethods": [
            "PUT",
            "POST",
            "GET"
        ],
        "AllowedOrigins": [
            "http://localhost:4200",
            "http://etalente-alb-665506398.af-south-1.elb.amazonaws.com"
        ],
        "ExposeHeaders": [
            "ETag"
        ]
    }
]
```

**Action Required:** The backend team needs to apply this CORS configuration to the `etalente-uploads` S3 bucket via the AWS console or their infrastructure-as-code scripts.