package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationType;
import java.time.Instant;
import java.util.UUID;

public record ServiceUnitLocationResponse(
        UUID id,
        UUID serviceUnitId,
        String name,
        String description,
        ServiceUnitLocationType type,
        boolean defaultLocation,
        String publicAccessSlug,
        ServiceUnitLocationStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ServiceUnitLocationResponse from(ServiceUnitLocation location) {
        return new ServiceUnitLocationResponse(
                location.getId(),
                location.getServiceUnit().getId(),
                location.getName(),
                location.getDescription(),
                location.getType(),
                location.isDefaultLocation(),
                location.getPublicAccessSlug(),
                location.getStatus(),
                location.getCreatedAt(),
                location.getUpdatedAt());
    }
}
