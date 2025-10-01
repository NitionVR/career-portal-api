package com.etalente.backend.controller;

import com.etalente.backend.config.TestTokenStore;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
@Profile("test")
public class TestHelperController {

    private final TestTokenStore tokenStore;

    public TestHelperController(TestTokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @GetMapping("/latest-registration-token")
    public ResponseEntity<Map<String, String>> getLatestRegistrationToken(@RequestParam String email) {
        String token = tokenStore.getToken(email);
        if (token == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("token", token));
    }
}
