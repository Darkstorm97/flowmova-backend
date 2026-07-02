package com.flowmova.backend.user.api;

import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.domain.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String profilePicture,
        String preferredLanguage,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getProfilePicture(),
                user.getPreferredLanguage(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
