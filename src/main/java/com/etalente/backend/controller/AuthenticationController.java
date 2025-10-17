package com.etalente.backend.controller;

import com.etalente.backend.dto.LoginRequest;
import com.etalente.backend.dto.SessionResponse;
import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.security.JwtService;
import com.etalente.backend.service.AuthenticationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    public AuthenticationController(AuthenticationService authenticationService,
                                   JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    /**
     * Step 1: Request magic link
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Magic link requested for email: {}", request.getEmail());
        authenticationService.sendMagicLink(request.getEmail());
        return ResponseEntity.ok(Map.of(
            "message", "Magic link sent to your email",
            "email", request.getEmail()
        ));
    }

    /**
     * Step 2: Verify OTT and return session JWT
     * GET /api/auth/verify?token=xxx
     */
    @GetMapping("/verify")
    public ResponseEntity<VerifyTokenResponse> verifyToken(@RequestParam String token) {
        logger.info("Verifying OTT token");
        VerifyTokenResponse response = authenticationService.exchangeOneTimeTokenForJwt(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Check if current session is valid
     * GET /api/auth/session
     */
    @GetMapping("/session")
    public ResponseEntity<SessionResponse> checkSession(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(SessionResponse.unauthenticated());
        }

        String token = authHeader.substring(7);

        try {
            String userId = jwtService.extractUserId(token);

            if (jwtService.isTokenValid(token, userId)) {
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);
                Boolean isNewUser = jwtService.extractIsNewUser(token);
                long expiresIn = jwtService.getTimeUntilExpiration(token);

                return ResponseEntity.ok(SessionResponse.authenticated(
                    userId, email, role, isNewUser, expiresIn
                ));
            }
        } catch (Exception e) {
            logger.warn("Session check failed: {}", e.getMessage());
        }

        return ResponseEntity.ok(SessionResponse.unauthenticated());
    }

    /**
     * Refresh session (extend expiration)
     * POST /api/auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @RequestHeader("Authorization") String authHeader) {

        String token = authHeader.substring(7);
        String userId = jwtService.extractUserId(token);
        String email = jwtService.extractEmail(token);
        String role = jwtService.extractRole(token);
        Boolean isNewUser = jwtService.extractIsNewUser(token);
        String username = jwtService.extractClaim(token, claims -> claims.get("username", String.class));


        // Generate new token with same claims
        String newToken = jwtService.generateToken(userId, email, role, isNewUser, username);

        return ResponseEntity.ok(Map.of(
            "token", newToken,
            "expiresIn", jwtService.getTimeUntilExpiration(newToken)
        ));
    }

    /**
     * Logout (client-side token deletion)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        logger.info("User logged out");
        return ResponseEntity.ok(Map.of(
            "message", "Logged out successfully"
        ));
    }
}
