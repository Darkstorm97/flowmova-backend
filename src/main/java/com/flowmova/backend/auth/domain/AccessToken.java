package com.flowmova.backend.auth.domain;

public record AccessToken(
        String value,
        long expiresInSeconds) {
}
