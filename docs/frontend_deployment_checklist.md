# Frontend Deployment Checklist for Etalente Backend Communication

This document outlines critical configuration steps required for the frontend application to successfully communicate with the backend when deployed to AWS ECS. These steps are essential for enabling API calls and ensuring proper routing.

## 1. Frontend Nginx Configuration (`nginx.conf`)

The `nginx.conf` file within your frontend Docker image **MUST** be updated to correctly proxy API requests to the backend service.

**Problem:** Your current `nginx.conf` does not include a `location /api/` block for proxying. The `proxy.conf.json` is only used by the Angular development server and is ignored by Nginx.

**Required Changes:**

*   **Add a `location /api/` block:** This block will intercept all requests to `/api/*` and forward them to the backend ECS service.
*   **Update `server_name`:** Change `server_name localhost;` to your frontend's actual domain name or `_` (wildcard) for a deployed environment.

**Example `nginx.conf` modification:**

```nginx
server {
  listen 80;
  server_name your-frontend-domain.com; # Replace with your actual domain, or use _ for wildcard

  root /usr/share/nginx/html;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;
  }

  location /api/ {
    # Proxy requests to the internal DNS name of the backend ECS service
    # The backend ECS service name is typically `${ProjectName}-backend-service`
    proxy_pass http://etalente-backend-service:8080; 
    
    # Essential headers for proper proxying
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # Add any other headers needed for backend communication (e.g., authorization)
  }
}
```

**Action:** Update the `nginx.conf` file in your frontend repository's source code. This change will be picked up when the frontend Docker image is rebuilt.

## 2. Backend CORS Configuration (`CORS_ALLOWED_ORIGINS`)

The backend's Cross-Origin Resource Sharing (CORS) configuration needs to explicitly allow requests from your deployed frontend domain.

**Problem:** If the `CORS_ALLOWED_ORIGINS` environment variable for the backend does not include your frontend's domain, the browser will block API requests due to CORS policy.

**Required Change:**

*   **Set `CORS_ALLOWED_ORIGINS`:** Ensure the `CORS_ALLOWED_ORIGINS` environment variable for the backend includes the full URL of your deployed frontend application (e.g., `https://your-frontend-domain.com`).

**Action:** Configure `CORS_ALLOWED_ORIGINS` as a **GitLab CI/CD variable** for the backend deployment.

---

**Note:** The `etalente-backend-service` in the `proxy_pass` example assumes that the backend ECS service is named `${ProjectName}-backend-service` and is discoverable via internal DNS within the same VPC. This is typically handled automatically by AWS ECS service discovery.

This checklist is crucial for enabling successful communication between your deployed frontend and backend services.
