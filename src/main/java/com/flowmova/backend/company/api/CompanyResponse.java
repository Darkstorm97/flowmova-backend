package com.flowmova.backend.company.api;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyStatus;
import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String description,
        String currency,
        CompanyStatus status,
        Instant createdAt,
        Instant updatedAt) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getCurrency(),
                company.getStatus(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}
