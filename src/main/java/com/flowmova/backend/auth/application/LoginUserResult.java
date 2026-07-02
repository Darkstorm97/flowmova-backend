package com.flowmova.backend.auth.application;

public record LoginUserResult(
        String accessToken,
        String tokenType,
        long expiresIn) {
}
