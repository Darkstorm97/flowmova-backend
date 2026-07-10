package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitCreationEntryMode;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
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
        TicketCreationGuardMode ticketCreationGuardMode,
        ServiceUnitCreationEntryMode creationEntryMode,
        boolean allowTicketWithoutItems,
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
                serviceUnit.getTicketCreationGuardMode(),
                serviceUnit.getCreationEntryMode(),
                serviceUnit.isTicketWithoutItemsAllowed(),
                serviceUnit.getCreatedAt(),
                serviceUnit.getUpdatedAt());
    }
}
