package com.flowmova.backend.item.application;

import com.flowmova.backend.item.domain.ItemAvailability;
import java.math.BigDecimal;

public record UpdateItemCommand(
        BigDecimal priceAmount,
        ItemAvailability availability,
        Integer configuredQuantity,
        Integer displayOrder) {
}
