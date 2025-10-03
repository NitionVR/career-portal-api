package com.etalente.backend.config;

import com.etalente.backend.service.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class TestTokenStore implements TokenStore {
    private static final Logger logger = LoggerFactory.getLogger(TestTokenStore.class);
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    @Override
    public void addToken(String email, String token) {
        logger.info("Storing token for email: {}", email);
        tokens.put(email, token);
    }

    @Override
    public String getToken(String email) {
        logger.info("Retrieving token for email: {}", email);
        return tokens.get(email);
    }

    public void clear() {
        tokens.clear();
    }
}