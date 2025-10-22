# S3 Pre-Signed URL Generation Fix

**Epic:** Complete File Upload Functionality

## Problem

The frontend is receiving a `SignatureDoesNotMatch` error from S3 when attempting to upload a file using a pre-signed URL. The error's `<CanonicalRequest>` shows that the signature was generated for a `GET` request, but the frontend is correctly attempting to make a `PUT` request.

This indicates that the backend service is generating pre-signed URLs for `GET` operations instead of `PUT` operations.

## Requirements

The backend logic for generating pre-signed URLs for file uploads must be updated to specify the `PUT` HTTP method.

When generating the pre-signed URL for both avatar and company logo uploads, the AWS SDK call must explicitly set the method to `PUT`.

### Example (using AWS SDK for Java):

```java
// Incorrect (or default) - Generates a GET URL
GeneratePresignedUrlRequest generatePresignedUrlRequest = 
    new GeneratePresignedUrlRequest(bucketName, objectKey);

// Correct - Generates a PUT URL
GeneratePresignedUrlRequest generatePresignedUrlRequest = 
    new GeneratePresignedUrlRequest(bucketName, objectKey)
        .withMethod(HttpMethod.PUT)
        .withExpiration(expiration);
```

**Action Required:** The backend team needs to review their pre-signed URL generation code (likely in the `ProfileController` and `OrganizationController`) and ensure that `HttpMethod.PUT` (or the equivalent for their SDK) is being specified for all upload URLs.
