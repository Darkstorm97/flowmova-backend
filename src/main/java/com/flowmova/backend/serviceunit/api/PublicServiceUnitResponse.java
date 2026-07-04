package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import java.time.Instant;
import java.util.UUID;

public record PublicServiceUnitResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        String location,
        ServiceUnitType type,
        ServiceUnitStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static PublicServiceUnitResponse from(ServiceUnit serviceUnit) {
        return new PublicServiceUnitResponse(
                serviceUnit.getId(),
                serviceUnit.getCompany().getId(),
                serviceUnit.getName(),
                serviceUnit.getDescription(),
                serviceUnit.getLocation(),
                serviceUnit.getType(),
                serviceUnit.getStatus(),
                serviceUnit.getCreatedAt(),
                serviceUnit.getUpdatedAt());
    }
}
