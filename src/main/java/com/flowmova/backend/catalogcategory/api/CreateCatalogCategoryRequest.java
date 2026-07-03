package com.flowmova.backend.catalogcategory.api;

import com.flowmova.backend.catalogcategory.application.CreateCatalogCategoryCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCatalogCategoryRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2_000) String description,
        @Min(0) Integer displayOrder) {

    public CreateCatalogCategoryCommand toCommand() {
        return new CreateCatalogCategoryCommand(name, description, displayOrder);
    }
}
