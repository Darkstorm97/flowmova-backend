package com.flowmova.backend.catalogcategory.application;

public record UpdateCatalogCategoryCommand(
        String name,
        String description,
        Integer displayOrder) {
}
