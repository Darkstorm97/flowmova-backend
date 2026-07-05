package com.flowmova.backend.company.application;

public record CreateCompanyCommand(
        String name,
        String description,
        String currency,
        String businessType) {
}
