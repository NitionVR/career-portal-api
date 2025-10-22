# Frontend File Upload Implementation & S3 Issue

This document details the end-to-end implementation of the file upload feature on the frontend and explains the persistent `SignatureDoesNotMatch` error.

---

## 1. User Interface (UI)

The UI for file uploads is located in `src/app/pages/talent/profile-page/profile-page.html`.

*   A hidden file input is used to trigger the browser's file selection dialog:
    ```html
    <input (change)="onAvatarUpload($event)" class="hidden" id="profile-picture-upload" type="file" accept="image/png, image/jpeg"/>
    ```
*   A styled `<label>` acts as the user-facing "Upload Picture" button.
*   The component also supports drag-and-drop via the `appFileDrop` directive.

## 2. Frontend Logic Flow

The entire logic is handled within `src/app/pages/talent/profile-page/profile-page.ts`.

### Step 2.1: File Selection and Validation

When a user selects a file, the `onAvatarUpload` method is called. It extracts the file and passes it to `handleAvatarUpload`.

```typescript
// Method called when the file input changes
  onAvatarUpload(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.handleAvatarUpload(file);
    }
  }
```

### Step 2.2: Get Pre-Signed URL from Backend

The `handleAvatarUpload` method initiates the process. It sets the loading state and calls the backend to get the secure upload URL.

```typescript
// 1. Starts the upload process and gets the pre-signed URL
  handleAvatarUpload(file: File): void {
    this.uploading = true;
    this.progress = 0; // Reset progress

    // Call the backend service to get the pre-signed URL
    this.profileService.getAvatarUploadUrl({
      body: {
        contentType: file.type,
        contentLength: file.size
      }
    }).subscribe({
      next: (response) => {
        if (response.uploadUrl && response.fileUrl) {
          // If successful, proceed to upload the file to S3
          this.uploadFileToCloud(file, response.uploadUrl, response.fileUrl);
        } else {
          console.error('Backend did not return a valid upload URL.');
          this.uploading = false;
        }
      },
      error: (err) => {
        console.error('Failed to get upload URL', err);
        this.uploading = false;
      }
    });
  }
```

### Step 2.3: Upload File to S3 (Using `fetch`)

To avoid issues with Angular's `HttpClient` interceptors modifying the request and invalidating the signature, we use the native `fetch` API to perform the upload. This sends a clean `PUT` request with the correct headers.

```typescript
// 2. Uploads the file directly to S3 using the pre-signed URL
  private async uploadFileToCloud(file: File, uploadUrl: string, fileUrl: string): Promise<void> {
    try {
      const response = await fetch(uploadUrl, {
        method: 'PUT',
        body: file,
        headers: {
          'Content-Type': file.type
        }
      });

      if (response.ok) {
        // If S3 upload is successful, notify our backend
        this.updateProfileAvatar(fileUrl);
      } else {
        console.error('Failed to upload file to cloud storage', response);
        this.uploading = false;
      }
    } catch (err) {
      console.error('Failed to upload file to cloud storage', err);
      this.uploading = false;
    }
  }
```

### Step 2.4: Finalize Upload with Backend

After the file is successfully in S3, the frontend notifies the backend by sending the final `fileUrl`. The backend then associates this URL with the user's profile.

```typescript
// 3. Notifies the backend that the upload is complete
  private updateProfileAvatar(fileUrl: string): void {
    this.profileService.updateAvatar({
      body: { profileImageUrl: fileUrl }
    }).subscribe({
      next: (updatedUser) => {
        // Update the user state locally for immediate UI feedback
        this.authService['storeUser'](updatedUser as User);
        this.authService['currentUserSubject'].next(updatedUser as User);
        this.uploading = false;
      },
      error: (err) => {
        console.error('Failed to update profile with new avatar URL', err);
        this.uploading = false;
      }
    });
  }
```

---

## 3. The Problem: `SignatureDoesNotMatch`

Despite the frontend implementation being correct and explicitly using the `PUT` method via `fetch`, the browser still receives a `403 Forbidden` error from AWS S3.

**Error Log from S3:**
```xml
<Error>
  <Code>SignatureDoesNotMatch</Code>
  <Message>The request signature we calculated does not match the signature you provided...</Message>
  <CanonicalRequest>GET /avatars/e1c03eba-c11d-425b-ae8a-3edd22a5f7a6.jpg...</CanonicalRequest>
</Error>
```

### Conclusion

The error log from S3 is definitive. The `<CanonicalRequest>` shows that the signature provided by the backend was generated for a **`GET`** request.

This proves the issue lies within the **backend's pre-signed URL generation logic**. The backend service is not correctly specifying `HttpMethod.PUT` when creating the URL for uploads.

**The frontend implementation is correct and robust. No further frontend changes can resolve this issue.** The backend team must correct their signature generation process.
