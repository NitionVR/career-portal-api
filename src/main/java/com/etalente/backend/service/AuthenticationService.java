package com.etalente.backend.service;

public interface AuthenticationService {
    void initiateMagicLinkLogin(String email);
}
