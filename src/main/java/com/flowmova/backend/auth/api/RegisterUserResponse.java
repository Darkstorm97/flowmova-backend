package com.flowmova.backend.auth.api;

import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record RegisterUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        Instant createdAt) {

    public static RegisterUserResponse from(User user) {
        return new RegisterUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                user.getCreatedAt());
    }
}
