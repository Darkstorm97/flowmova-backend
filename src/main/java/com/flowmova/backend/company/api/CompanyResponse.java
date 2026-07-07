package com.flowmova.backend.company.api;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyBusinessType;
import com.flowmova.backend.company.domain.CompanyOperationalStatus;
import com.flowmova.backend.company.domain.CompanyStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CompanyResponse(
        UUID id,
        String name,
        String description,
        String imageUrl,
        String currency,
        CompanyBusinessType businessType,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        CompanyStatus status,
        CompanyOperationalStatus operationalStatus,
        Instant createdAt,
        Instant updatedAt) {

    public static CompanyResponse from(Company company) {
        return new CompanyResponse(
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getImageUrl(),
                company.getCurrency(),
                company.getBusinessType(),
                company.getAddressLine1(),
                company.getAddressLine2(),
                company.getCity(),
                company.getRegion(),
                company.getPostalCode(),
                company.getCountry(),
                company.getLatitude(),
                company.getLongitude(),
                company.getStatus(),
                company.getOperationalStatus(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}
