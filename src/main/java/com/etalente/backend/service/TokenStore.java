package com.etalente.backend.service;

public interface TokenStore {
    void addToken(String key, String token);
}
