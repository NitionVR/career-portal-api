package com.etalente.backend.config;

import com.etalente.backend.service.TokenStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class TestTokenStore implements TokenStore {

    private final Map<String, String> registrationTokens = new ConcurrentHashMap<>();

    @Override
    public void addToken(String email, String token) {
        registrationTokens.put(email, token);
    }

    public String getToken(String email) {
        return registrationTokens.get(email);
    }

    public void clear() {
        registrationTokens.clear();
    }
}
