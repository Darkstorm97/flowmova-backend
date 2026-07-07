package com.flowmova.backend.company.api;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyBusinessType;
import com.flowmova.backend.company.domain.CompanyOperationalStatus;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CurrentUserCompanyResponse(
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
        CompanyRole role,
        Instant createdAt,
        Instant updatedAt) {

    public static CurrentUserCompanyResponse from(CompanyUser companyUser) {
        Company company = companyUser.getCompany();
        return new CurrentUserCompanyResponse(
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
                companyUser.getRole(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}
