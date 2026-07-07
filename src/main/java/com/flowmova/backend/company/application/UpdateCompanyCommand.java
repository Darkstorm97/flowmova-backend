package com.flowmova.backend.company.application;

import java.math.BigDecimal;

public record UpdateCompanyCommand(
        String name,
        String description,
        String imageUrl,
        String currency,
        String businessType,
        String operationalStatus,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude) {
}
