package com.flowmova.backend.catalogcategory.api;

import com.flowmova.backend.catalogcategory.application.UpdateCatalogCategoryCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCatalogCategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2_000) String description,
        @Min(0) Integer displayOrder) {

    public UpdateCatalogCategoryCommand toCommand() {
        return new UpdateCatalogCategoryCommand(name, description, displayOrder);
    }
}
