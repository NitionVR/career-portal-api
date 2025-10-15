package com.etalente.backend.service;

import com.etalente.backend.model.User;

public interface AuthenticationService {
    void initiateMagicLinkLogin(String email);
    String exchangeOneTimeTokenForJwt(String ott);
    String generateJwtForUser(User user);
}
