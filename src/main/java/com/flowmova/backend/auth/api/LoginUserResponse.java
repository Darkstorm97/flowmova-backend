package com.flowmova.backend.auth.api;

import com.flowmova.backend.auth.application.LoginUserResult;

public record LoginUserResponse(
        String accessToken,
        String tokenType,
        long expiresIn) {

    public static LoginUserResponse from(LoginUserResult result) {
        return new LoginUserResponse(result.accessToken(), result.tokenType(), result.expiresIn());
    }
}
