# AWS Infrastructure Improvements and Manual Configuration Checklist

This document outlines recommended improvements for production readiness and manual configuration steps required when deploying the Etalente backend to a new AWS account.

## 1. GitLab CI/CD Variables (Protected where sensitive)

These variables need to be configured in your GitLab project's CI/CD settings. Sensitive variables should be marked as "protected" and "masked".

*   **`AWS_ACCESS_KEY_ID`**: Your AWS Access Key ID for the deployment user.
*   **`AWS_SECRET_ACCESS_KEY`**: Your AWS Secret Access Key for the deployment user (protected).
*   **`AWS_REGION`**: The AWS region where you intend to deploy (e.g., `af-south-1`, `us-east-1`).
*   **`AWS_ACCOUNT_ID`**: Your 12-digit AWS Account ID.
*   **`DB_MASTER_PASSWORD`**: A strong, unique password for the RDS PostgreSQL master user (protected).
*   **`JWT_SECRET`**: A strong, unique secret key for signing JWTs (protected).
*   **`MAGIC_LINK_URL`**: The base URL for magic links, typically your frontend application's domain (e.g., `https://your-frontend-domain.com`).
*   **`INVITATION_LINK_URL`**: The base URL for invitation links, typically your frontend application's domain (e.g., `https://your-frontend-domain.com`).
*   **`ProjectName`**: (Optional) If you wish to change the default project name from `etalente`, update this variable. This will affect CloudFormation stack names and resource names.
*   **`GITHUB_EMAIL`**, **`GITHUB_USERNAME`**, **`GITHUB_TOKEN`**: (Optional) If you intend to use the GitHub mirroring feature, these variables need to be configured.

## 2. CloudFormation Template Modifications (for Production Readiness)

These changes involve modifying the CloudFormation templates directly to enhance security, availability, and cost-effectiveness for a production environment.

### `aws/cloudformation/shared/networking.yaml`

*   **Availability Zones:** Update the `AvailabilityZone1` and `AvailabilityZone2` parameters to valid Availability Zones within your chosen `AWS_REGION`. The current defaults (`af-south-1a`, `af-south-1b`) are specific to the Cape Town region.

### `aws/cloudformation/shared/rds.yaml`

*   **High Availability:** Change `MultiAZ` from `false` to `true` to enable multi-AZ deployment for high availability.
*   **Deletion Protection:** Change `DeletionProtection` from `false` to `true` to prevent accidental deletion of the production database.
*   **Instance Class & Storage:** Review `DBInstanceClass` and `DBAllocatedStorage` parameters. Select an instance class and allocate storage appropriate for your expected production workload and performance requirements.
*   **Database Username:** Consider changing `DBMasterUsername` from the default `postgres` to a more specific and less privileged username for enhanced security in production.

### `aws/cloudformation/shared/s3.yaml`

*   **CORS Configuration:** Restrict `CorsConfiguration.AllowedOrigins` from `*` (all origins) to your specific frontend domain(s) (e.g., `https://your-frontend-domain.com`). This is crucial to prevent Cross-Site Scripting (XSS) vulnerabilities.
*   **Public Read Access:** Re-evaluate if all content uploaded to this bucket should be publicly readable. If sensitive user uploads are expected, implement a more granular access control strategy (e.g., using signed URLs).

### `aws/cloudformation/shared/alb.yaml`

*   **HTTPS Listener:** Add an HTTPS listener on port 443. This requires:
    1.  **Manual ACM Certificate Creation:** Obtain or import an SSL/TLS certificate in AWS Certificate Manager (ACM) for your domain in the target region.
    2.  **Template Modification:** Add an `AWS::ElasticLoadBalancingV2::Listener` resource for port 443, referencing the ARN of your ACM certificate.

### `aws/cloudformation/application/ecs-service.yaml`

*   **SES Permissions:** Restrict the `SesSendEmailPolicy` resource from `*` (all resources) to specific verified SES identities (email addresses or domains) in your new AWS account. This is a security best practice to limit the scope of permissions.

## Deployment Order

Ensure that the shared CloudFormation stacks are deployed in the correct order before the application-specific stack:

1.  `networking.yaml`
2.  `security-groups.yaml`
3.  `rds.yaml`
4.  `s3.yaml`
5.  `ecr-repositories.yaml`
6.  `ecs-cluster.yaml`
7.  `ecs-service.yaml` (application stack)

This checklist should help ensure a secure and robust deployment to a new AWS account.