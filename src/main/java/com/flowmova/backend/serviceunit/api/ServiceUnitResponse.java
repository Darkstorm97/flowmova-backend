package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitCreationEntryMode;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import java.time.Instant;
import java.util.UUID;

public record ServiceUnitResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        String location,
        ServiceUnitType type,
        ServiceUnitStatus status,
        TicketCreationGuardMode ticketCreationGuardMode,
        ServiceUnitCreationEntryMode creationEntryMode,
        boolean allowTicketWithoutItems,
        ServiceUnitLocationResponse defaultLocation,
        Instant createdAt,
        Instant updatedAt) {

    public static ServiceUnitResponse from(ServiceUnit serviceUnit, ServiceUnitLocation defaultLocation) {
        return new ServiceUnitResponse(
                serviceUnit.getId(),
                serviceUnit.getCompany().getId(),
                serviceUnit.getName(),
                serviceUnit.getDescription(),
                serviceUnit.getLocation(),
                serviceUnit.getType(),
                serviceUnit.getStatus(),
                serviceUnit.getTicketCreationGuardMode(),
                serviceUnit.getCreationEntryMode(),
                serviceUnit.isTicketWithoutItemsAllowed(),
                defaultLocation == null ? null : ServiceUnitLocationResponse.from(defaultLocation),
                serviceUnit.getCreatedAt(),
                serviceUnit.getUpdatedAt());
    }
}
