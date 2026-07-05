package com.flowmova.backend.company.api;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyBusinessType;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import java.time.Instant;
import java.util.UUID;

public record CurrentUserCompanyResponse(
        UUID id,
        String name,
        String description,
        String currency,
        CompanyBusinessType businessType,
        CompanyStatus status,
        CompanyRole role,
        Instant createdAt,
        Instant updatedAt) {

    public static CurrentUserCompanyResponse from(CompanyUser companyUser) {
        Company company = companyUser.getCompany();
        return new CurrentUserCompanyResponse(
                company.getId(),
                company.getName(),
                company.getDescription(),
                company.getCurrency(),
                company.getBusinessType(),
                company.getStatus(),
                companyUser.getRole(),
                company.getCreatedAt(),
                company.getUpdatedAt());
    }
}
