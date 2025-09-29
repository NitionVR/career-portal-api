# Etalente-clone Backend

This repository will house the backend API services for the Etalente-clone talent acquisition platform. It will provide core functionalities such as user management, job posting, and application processing.

## Purpose
This project is responsible for the business logic, data persistence, and API endpoints that will be consumed by the `etalente-clone-frontend` application and potentially other clients.

## Planned Technologies
*   **Framework:** Spring Boot 3+
*   **Language:** Java 17+
*   **Build Tool:** Gradle
*   **Database:** PostgreSQL
*   **Authentication:** JWT, Magic Links

## Getting Started (Initial Setup)
1.  **Clone the repository:**
    ```bash
    git clone <backend_repo_url>
    cd etalente-clone-backend
    ```
2.  **Initial Dependencies (Placeholder):**
    *   Java Development Kit (JDK) (LTS version recommended)
    *   Gradle

    _Note: Full application setup, database configuration, and run instructions will be added once the project structure and initial code are in place._

## Commit Strategy (Conventional Commits)

We follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification for our commit messages. This helps in creating an explicit development history, enabling automated changelog generation, and simplifying semantic versioning.

**Format:** `<type>[optional scope]: <description>`

**Example:**
```
feat(user): implement user registration endpoint
fix(db): correct migration script for job_posts table
docs(readme): add initial setup instructions
```

**Common Types:**
*   `feat`: A new feature
*   `fix`: A bug fix
*   `docs`: Documentation only changes
*   `style`: Changes that do not affect the meaning of the code (white-space, formatting, missing semicolons, etc.)
*   `refactor`: A code change that neither fixes a bug nor adds a feature
*   `perf`: A code change that improves performance
*   `test`: Adding missing tests or correcting existing tests
*   `build`: Changes that affect the build system or external dependencies (example scopes: gulp, broccoli, npm)
*   `ci`: Changes to our CI configuration files and scripts (example scopes: Travis, Circle, BrowserStack, SauceLabs)
*   `chore`: Other changes that don't modify src or test files
*   `revert`: Reverts a previous commit

## License
MIT License

## Author

