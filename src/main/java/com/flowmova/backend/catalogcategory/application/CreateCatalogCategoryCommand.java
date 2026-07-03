package com.flowmova.backend.catalogcategory.application;

public record CreateCatalogCategoryCommand(
        String name,
        String description,
        Integer displayOrder) {
}
