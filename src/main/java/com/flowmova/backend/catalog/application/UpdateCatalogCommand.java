package com.flowmova.backend.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCatalogCommand(
        UUID catalogCategoryId,
        String name,
        String description,
        String imageUrl,
        BigDecimal priceAmount) {
}
