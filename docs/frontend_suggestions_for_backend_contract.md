Backend API Updates for Frontend Integration

  Here are the necessary and recommended changes for the backend API to support the new, robust authentication flow.

  Part 1: High-Priority Requirements (Must-Haves)

  These changes are required to fix the current registration flow and prevent users from getting stuck.

  1.1. Add `action=register` to Registration Magic Links

   * Problem: The frontend cannot distinguish between a magic link for a new user registration and a magic link for an existing user login.
   * Requirement: When the backend sends a magic link for a brand new user registration, it must append the query parameter action=register to the callback URL.
   * Example URLs:
       * For Login: http://localhost:4200/auth/callback?token=...
       * For New Registration: http://localhost:4200/auth/callback?token=...&action=register
   * Impact: Without this, the frontend cannot correctly route new users to the /profile/create page, breaking the entire registration flow.

  1.2. Create an Endpoint to Complete a Profile for an Authenticated New User

   * Problem: A user who logs in for the first time (newUser: true) but closes the tab before completing their profile gets stuck. When they return, they are logged in but 
     cannot complete their profile because the existing complete...Registration endpoints require a one-time token they no longer have.
   * Requirement: Create a new, authenticated endpoint that allows a logged-in user to submit their initial profile details.
   * Suggested Endpoint: PUT /api/profile/me or POST /api/profile/complete
   * Details:
       * This endpoint must be protected and require the user's session JWT (Authorization: Bearer <token>).
       * It should accept a body similar to the CandidateRegistrationDto or HiringManagerRegistrationDto (e.g., firstName, lastName, companyName).
       * Upon success, it should update the user's details in the database and, crucially, return a new JWT with the newUser claim now set to false.
   * Impact: This is critical for fixing the "stuck new user" bug and making the user experience seamless.

  ---

  Part 2: Recommended Improvements (Should-Haves)

  These changes will improve security and user experience.

  2.1. Enhance the `/api/auth/session` Endpoint

   * Problem: The current GET /api/auth/session endpoint only returns basic user info (userId, email, role). It does not return the full user profile (like firstName, 
     lastName). This forces the frontend to rely on localStorage, which can become stale.
   * Recommendation: The /api/auth/session endpoint should return the full `UserDto` object, identical to the user object returned by the /api/auth/verify endpoint.
   * Impact: This will ensure the frontend always has the freshest user data on app load, preventing inconsistencies.

  2.2. Implement Server-Side Logout (Token Blacklisting)

   * Problem: The current POST /api/auth/logout endpoint is a no-op. When a user logs out, the frontend deletes the token, but the token itself remains valid until it 
     expires. If a token is stolen before logout, it can still be used.
   * Recommendation: Implement a token blacklisting mechanism.
       * When a user calls /api/auth/logout, add the token's unique identifier (the jti claim) to a short-lived blacklist (e.g., in a Redis cache with a TTL matching the 
         token's remaining validity).
       * The JwtAuthenticationFilter should be updated to check this blacklist on every incoming request. If the token is on the list, the request should be rejected with a 
         401 Unauthorized status.
   * Impact: This is a significant security enhancement that ensures logging out immediately invalidates the session on the server.