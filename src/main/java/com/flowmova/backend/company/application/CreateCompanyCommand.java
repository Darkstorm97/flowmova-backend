package com.flowmova.backend.company.application;

import java.math.BigDecimal;

public record CreateCompanyCommand(
        String name,
        String description,
        String currency,
        String businessType,
        String addressLine1,
        String addressLine2,
        String city,
        String region,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude) {
}
