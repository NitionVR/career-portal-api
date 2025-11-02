<h1>Career Portal API</h1>

<p align="center">
  <strong>A modern, robust backend for a next-generation talent acquisition platform.</strong>
</p>

<p align="center">
  <a href="https://github.com/novuhq/novu-java/releases/latest"><img src="https://img.shields.io/badge/Java-21-blue.svg" alt="Java 21"></a>
  <a href="#"><img src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg" alt="Spring Boot 3.x"></a>
  <a href="#"><img src="https://img.shields.io/badge/PostgreSQL-15-blue.svg" alt="PostgreSQL 15"></a>
  <a href="#"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License"></a>
</p>

---

This repository contains the backend services for the **Career Portal** platform. It provides the core business logic, data persistence, and a comprehensive API for user management, job postings, applicant tracking, and notifications, consumed by the `career-portal-frontend` application.

## Key Features

- **Passwordless Authentication**: Secure and seamless magic link login flow using One-Time Tokens (OTT) and JSON Web Tokens (JWT) for session management.
- **Multi-Tenant by Organization**: A clear separation of data and permissions between different employer organizations.
- **Comprehensive Job & Application Management**: Full lifecycle management for job posts (Draft, Open, Closed) and applications, complete with state auditing.
- **Advanced Applicant Filtering**: Powerful server-side search and filtering capabilities for employers to find the right candidates.
- **Role-Based Access Control (RBAC)**: Granular permissions for `CANDIDATE`, `RECRUITER`, and `HIRING_MANAGER` roles.
- **External Service Integrations**:
    - **AWS S3**: For secure, direct-to-cloud file uploads (resumes, logos) using pre-signed URLs.
    - **Novu**: Orchestrates multi-channel notifications (email, in-app) for key application events.
- **Infrastructure as Code (IaC)**: The entire AWS infrastructure is defined and version-controlled using CloudFormation.
- **Automated CI/CD**: A robust GitLab CI/CD pipeline automates testing, security scanning, containerization, and deployment to AWS ECS.

## Tech Stack

- **Framework**: Spring Boot 3+
- **Language**: Java 21
- **Build Tool**: Gradle
- **Database**: PostgreSQL 15 with Flyway for migrations
- **Authentication**: Spring Security, JWT
- **Cloud**: AWS (ECS Fargate, RDS, S3, ECR, ALB)
- **Containerization**: Docker
- **Testing**: JUnit 5, Mockito, Testcontainers, Karate (for E2E API tests)
- **Notifications**: [Novu](https://novu.co/)

## Architecture Overview

The Career Portal API is built on a classic **layered architecture** (`Controller` → `Service` → `Repository`).

- **Multi-Tenancy**: Tenancy is handled at the application level, with an `OrganizationContext` ensuring that all data access is scoped to the current user's organization.
- **State Machines**: Core entities like `JobPost` and `JobApplication` have their lifecycles managed by state machines, with all transitions recorded in audit tables for full traceability.
- **Notifications**: The system is event-driven. Services trigger notification events (e.g., `application-received`), which are then orchestrated and delivered by Novu via workflows defined in the codebase.
- **File Uploads**: To reduce server load and enhance security, the backend generates pre-signed URLs, allowing the client to upload files directly to AWS S3.

For a more detailed breakdown, see the [Codebase Overview](./docs/codebase_overview.md).

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose
- An AWS account (for S3/Novu integrations if not using mocks)

### Local Development

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/career-portal-api.git
    cd career-portal-api
    ```

2.  **Set Up Environment Variables:**
    Create an `.env` file in the root directory and populate it with the necessary secrets. Refer to the [Required Environment Variables](./docs/required_environment_variables.md) guide for a complete list.
    
    A minimal local `.env` file would look like this:
    ```env
    DB_MASTER_PASSWORD=postgres
    JWT_SECRET=your-super-secret-key-that-is-long-and-random
    NOVU_API_KEY=your-novu-api-key
    # For local email testing with MailDev
    SPRING_MAIL_USERNAME=
    SPRING_MAIL_PASSWORD=
    SPRING_MAIL_FROM=no-reply@careerportal.com
    ```

3.  **Start Local Dependencies:**
    The `docker-compose.yml` file will spin up a PostgreSQL database and a MailDev server for catching outbound emails.
    ```bash
    docker-compose up -d
    ```

4.  **Run the Application:**
    Use the Gradle wrapper to run the application. It will automatically be configured to connect to the local Docker containers.
    ```bash
    ./gradlew bootRun
    ```
    The API will be available at `http://localhost:8080`.

## Testing

The project has a comprehensive test suite separated into three main categories.

- **Unit Tests**:
  ```bash
  ./gradlew test
  ```

- **Integration Tests** (uses Testcontainers):
  ```bash
  ./gradlew integrationTest
  ```

- **Acceptance (E2E) Tests** (uses Karate):
  ```bash
  ./gradlew acceptanceTest
  ```

- **Run All Checks**: 
  ```bash
  ./gradlew check
  ```

## API Documentation

The API is documented via Postman collections and detailed markdown files.

- **Postman Collections**:
  - [Etalente_Backend_Testing.postman_collection.json](./Etalente_Backend_Testing.postman_collection.json)
  - [Etalente_Backend_Testing_with_Novu.postman_collection.json](./Etalente_Backend_Testing_with_Novu.postman_collection.json)
- **API Contracts**: For detailed request/response formats, see the documents in the [`docs/`](./docs/) directory, especially under `frontend_api_updates/` and `registration_api_contract.md`.

## CI/CD Pipeline

The project uses GitLab CI for continuous integration and deployment. The pipeline, defined in `.gitlab-ci.yml`, consists of the following stages:

1.  **`security_scan`**: Runs `gitleaks` to detect hardcoded secrets.
2.  **`test`**: Executes unit and integration tests.
3.  **`build_image`**: Builds the Docker image and pushes it to AWS ECR.
4.  **`test_e2e`**: Runs end-to-end Karate tests against the newly built container.
5.  **`validate_iac`**: Validates the AWS CloudFormation templates.
6.  **`deploy_infrastructure`**: Deploys shared AWS resources.
7.  **`deploy_app`**: Deploys the application to AWS ECS.
8.  **`mirror`**: Mirrors the code to a GitHub repository.

## Contributing

We follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification for our commit messages. This helps in creating an explicit development history and enables automated changelogs.

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change. Please make sure to update tests as appropriate.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
