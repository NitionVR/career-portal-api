package com.etalente.backend.service;

import com.etalente.backend.model.User;

public interface AuthenticationService {
    void initiateMagicLinkLogin(String email);
    String verifyMagicLinkAndIssueJwt(String token);
    String generateJwtForUser(User user);
}
