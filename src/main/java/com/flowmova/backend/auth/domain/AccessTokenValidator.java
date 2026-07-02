package com.flowmova.backend.auth.domain;

public interface AccessTokenValidator {

    AuthenticatedUser validate(String token);
}
