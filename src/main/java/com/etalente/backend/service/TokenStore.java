package com.etalente.backend.service;

public interface TokenStore {
    void addToken(String email, String token);
    String getToken(String email);
}