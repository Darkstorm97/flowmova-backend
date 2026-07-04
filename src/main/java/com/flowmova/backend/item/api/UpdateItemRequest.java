package com.flowmova.backend.item.api;

import com.flowmova.backend.item.application.UpdateItemCommand;
import com.flowmova.backend.item.domain.ItemAvailability;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateItemRequest(
        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceAmount,
        @NotNull ItemAvailability availability,
        @Min(0) Integer configuredQuantity,
        @NotNull @Min(0) Integer displayOrder) {

    public UpdateItemCommand toCommand() {
        return new UpdateItemCommand(priceAmount, availability, configuredQuantity, displayOrder);
    }
}
