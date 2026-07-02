package com.flowmova.backend.auth.application;

public record LoginUserCommand(
        String email,
        String password) {
}
