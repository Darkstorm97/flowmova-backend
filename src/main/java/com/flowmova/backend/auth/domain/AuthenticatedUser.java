package com.flowmova.backend.auth.domain;

import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String email) {
}
