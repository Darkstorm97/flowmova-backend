package com.flowmova.backend.catalog.api;

import com.flowmova.backend.catalog.application.UpdateCatalogCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCatalogRequest(
        @NotNull UUID catalogCategoryId,
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2_000) String description,
        @Size(max = 2_000) String imageUrl,
        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal priceAmount) {

    public UpdateCatalogCommand toCommand() {
        return new UpdateCatalogCommand(catalogCategoryId, name, description, imageUrl, priceAmount);
    }
}
