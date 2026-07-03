package com.flowmova.backend.catalog.application;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCatalogCommand(
        UUID catalogCategoryId,
        String name,
        String description,
        String imageUrl,
        BigDecimal priceAmount) {
}
