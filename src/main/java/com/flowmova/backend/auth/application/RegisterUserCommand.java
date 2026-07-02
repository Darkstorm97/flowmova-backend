package com.flowmova.backend.auth.application;

public record RegisterUserCommand(
        String email,
        String password,
        String firstName,
        String lastName) {
}
