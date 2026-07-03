package com.flowmova.backend.catalogcategory.api;

import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.domain.CatalogCategoryStatus;
import java.time.Instant;
import java.util.UUID;

public record CatalogCategoryResponse(
        UUID id,
        UUID companyId,
        String name,
        String description,
        Integer displayOrder,
        CatalogCategoryStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static CatalogCategoryResponse from(CatalogCategory category) {
        return new CatalogCategoryResponse(
                category.getId(),
                category.getCompany().getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.getStatus(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
