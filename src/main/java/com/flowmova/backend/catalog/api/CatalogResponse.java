package com.flowmova.backend.catalog.api;

import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.domain.CatalogStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CatalogResponse(
        UUID id,
        UUID companyId,
        UUID catalogCategoryId,
        String name,
        String description,
        String imageUrl,
        BigDecimal priceAmount,
        CatalogStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static CatalogResponse from(Catalog catalog) {
        return new CatalogResponse(
                catalog.getId(),
                catalog.getCompany().getId(),
                catalog.getCatalogCategory().getId(),
                catalog.getName(),
                catalog.getDescription(),
                catalog.getImageUrl(),
                catalog.getPriceAmount(),
                catalog.getStatus(),
                catalog.getCreatedAt(),
                catalog.getUpdatedAt());
    }
}
