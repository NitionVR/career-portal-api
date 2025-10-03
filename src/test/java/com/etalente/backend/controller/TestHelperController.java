package com.etalente.backend.controller;

import com.etalente.backend.config.TestTokenStore;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@Profile("test")
public class TestHelperController {

    private final TestTokenStore tokenStore;

    public TestHelperController(TestTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @GetMapping("/token")
    public Map<String, String> getToken(@RequestParam String email) {
        String token = tokenStore.getToken(email);
        if (token != null) {
            return Map.of("token", token);
        }
        // Return a 404 or a more specific error in a real app
        throw new RuntimeException("No token found for email: " + email);
    }
}