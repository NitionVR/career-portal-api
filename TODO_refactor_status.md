# Refactoring Status and TODO

This document summarizes the current state of the authentication refactoring work and outlines the immediate next steps required to get the build to a stable, passing state.

### Summary of Work

**Goal:** We have been refactoring the application's authentication and session management system based on the specifications in `docs/tentative_security_improvements.log`.

**Key Changes Implemented:**
*   The `JwtService` was updated to include richer claims (role, username, etc.).
*   The authentication principal was changed from the user's `email` (String) to their `ID` (UUID).
*   New DTOs (`VerifyTokenResponse`, `SessionResponse`) were created.
*   The `AuthenticationController` was rewritten with new endpoints (`/session`, `/refresh`, `/verify`).
*   Registration logic was moved to a new, dedicated `RegistrationController`.
*   `SecurityConfig` and `application.yml` files were updated to be more secure and organized.
*   A `TestHelper` class was created to centralize user/token creation for integration tests.
*   The `BaseIntegrationTest` was updated to use the new `UUID`-based authentication principal.

### Current Problem

A large number of integration tests are failing due to the significant refactoring. The root cause is that many test classes still contain outdated code that is not compatible with the new authentication system (i.e., they still try to use `email` instead of `UUID` for user identification and authentication).

### Next Steps to Fix the Build

The immediate task is to systematically refactor all remaining failing integration test classes. The general solution for each failing test class is:

1.  **Inject the `TestHelper`:** Add `@Autowired private TestHelper testHelper;`
2.  **Use the `TestHelper`:** Replace all manual `User` creation and JWT generation with calls to `testHelper.createUser()` and `testHelper.createUserAndGetJwt()`.
3.  **Update Authentication:** Ensure that the `authenticateAs()` method is called with the user's `UUID` (`user.getId()`), not their email.
4.  **Update Service Calls:** Ensure all calls to services (like `jobPostService`, `invitationService`, etc.) that previously took an email now pass the user's `UUID`.

The following test files are known to still have failures and need to be refactored using the pattern described above:
*   `JobPostPermissionControllerTest.java`
*   `JobPostServiceImplTest.java`
*   `JobPostMultiTenancyTest.java`
*   `JobPostPermissionErrorHandlingTest.java`
*   `JobPostPermissionTest.java`

Once these test classes are updated, the build should pass.
