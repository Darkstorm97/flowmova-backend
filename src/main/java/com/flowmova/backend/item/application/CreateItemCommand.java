package com.flowmova.backend.item.application;

import com.flowmova.backend.item.domain.ItemAvailability;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateItemCommand(
        UUID catalogId,
        BigDecimal priceAmount,
        ItemAvailability availability,
        Integer configuredQuantity,
        Integer displayOrder) {
}
