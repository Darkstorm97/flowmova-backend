package com.flowmova.backend.item.api;

import com.flowmova.backend.catalog.api.CatalogResponse;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.domain.ItemStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ItemResponse(
        UUID id,
        UUID serviceUnitId,
        CatalogResponse catalog,
        BigDecimal priceAmount,
        ItemAvailability availability,
        Integer configuredQuantity,
        Integer reservedQuantity,
        Integer displayOrder,
        ItemStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getServiceUnit().getId(),
                CatalogResponse.from(item.getCatalog()),
                item.getPriceAmount(),
                item.getAvailability(),
                item.getConfiguredQuantity(),
                item.getReservedQuantity(),
                item.getDisplayOrder(),
                item.getStatus(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
