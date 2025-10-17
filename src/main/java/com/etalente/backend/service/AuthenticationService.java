package com.etalente.backend.service;

import com.etalente.backend.dto.VerifyTokenResponse;
import com.etalente.backend.model.User;

public interface AuthenticationService {
    void sendMagicLink(String email);
    VerifyTokenResponse exchangeOneTimeTokenForJwt(String ott);
    String generateJwtForUser(User user);
}
