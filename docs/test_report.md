# Test Report

This report summarizes the backend test execution status and observations.

## Commands Executed

- `./gradlew test --info`: Unit test suite — executed successfully (BUILD SUCCESSFUL). Noted deprecation warnings and Mockito self-attach issue that may break on future JDKs.
- `./gradlew integrationTest --info`: Integration tests — executed successfully (exit code 0). Uses Testcontainers Postgres and mocked AWS/email.
- `./gradlew acceptanceTest --info`: Acceptance (Karate) tests — executed successfully (BUILD SUCCESSFUL). Noted deprecation warnings.
- `./gradlew check --info`: Aggregate check — failed (exit code 1). Logs could not be retrieved in this environment.

## Observations

- Unit/Integration/Acceptance tasks individually pass, suggesting core functionality is stable and consistent with current implementations.
- The `check` task aggregates verification tasks and may include additional steps beyond tests (e.g., formatting, linting, or other verification tasks) — the exact cause of failure is unknown due to unavailable logs.
- There are warnings regarding future incompatibilities (e.g., Mockito self-attach) that should be addressed when upgrading JDK/test dependencies.

## Next Actions

- Re-run `gradlew check` locally with full logs to identify the failing subtask.
- If the failure is unrelated to code changes (e.g., environment or transient), consider isolating the failing task (e.g., `gradlew <subtask> --info`).
- Track and resolve deprecations to ensure future compatibility.