package com.flowmova.backend.company.api;

import com.flowmova.backend.company.application.CreateCompanyCommand;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2_000) String description,
        @Size(max = 2_000) String imageUrl,
        @Size(max = 3) String currency,
        @Size(max = 50) String businessType,
        @Size(max = 20) String operationalStatus,
        @Size(max = 255) String addressLine1,
        @Size(max = 255) String addressLine2,
        @Size(max = 120) String city,
        @Size(max = 120) String region,
        @Size(max = 40) String postalCode,
        String country,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude) {

    public CreateCompanyCommand toCommand() {
        return new CreateCompanyCommand(
                name,
                description,
                imageUrl,
                currency,
                businessType,
                operationalStatus,
                addressLine1,
                addressLine2,
                city,
                region,
                postalCode,
                country,
                latitude,
                longitude);
    }
}
