package com.flowmova.backend.item.api;

import com.flowmova.backend.item.application.CreateItemCommand;
import com.flowmova.backend.item.domain.ItemAvailability;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateItemRequest(
        @NotNull UUID catalogId,
        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceAmount,
        ItemAvailability availability,
        @Min(0) Integer configuredQuantity,
        @Min(0) Integer displayOrder) {

    public CreateItemCommand toCommand() {
        return new CreateItemCommand(catalogId, priceAmount, availability, configuredQuantity, displayOrder);
    }
}
