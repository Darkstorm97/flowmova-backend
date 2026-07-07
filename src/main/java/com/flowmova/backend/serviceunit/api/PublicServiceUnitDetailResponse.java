package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.item.api.ItemResponse;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitCreationEntryMode;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublicServiceUnitDetailResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        String location,
        ServiceUnitType type,
        ServiceUnitStatus status,
        TicketCreationGuardMode ticketCreationGuardMode,
        ServiceUnitCreationEntryMode creationEntryMode,
        ServiceUnitLocationResponse defaultLocation,
        List<ServiceUnitLocationResponse> locations,
        List<ItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static PublicServiceUnitDetailResponse from(
            ServiceUnit serviceUnit,
            ServiceUnitLocation defaultLocation,
            List<ServiceUnitLocation> locations,
            List<Item> items) {
        return new PublicServiceUnitDetailResponse(
                serviceUnit.getId(),
                serviceUnit.getCompany().getId(),
                serviceUnit.getName(),
                serviceUnit.getDescription(),
                serviceUnit.getLocation(),
                serviceUnit.getType(),
                serviceUnit.getStatus(),
                serviceUnit.getTicketCreationGuardMode(),
                serviceUnit.getCreationEntryMode(),
                ServiceUnitLocationResponse.from(defaultLocation),
                locations.stream()
                        .map(ServiceUnitLocationResponse::from)
                        .toList(),
                items.stream()
                        .map(ItemResponse::from)
                        .toList(),
                serviceUnit.getCreatedAt(),
                serviceUnit.getUpdatedAt());
    }
}
