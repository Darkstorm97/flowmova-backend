package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.company.api.CompanyResponse;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;

public record PublicLocationAccessResponse(
        CompanyResponse company,
        PublicServiceUnitResponse serviceUnit,
        ServiceUnitLocationResponse location) {

    public static PublicLocationAccessResponse from(
            Company company,
            ServiceUnit serviceUnit,
            ServiceUnitLocation location) {
        return new PublicLocationAccessResponse(
                CompanyResponse.from(company),
                PublicServiceUnitResponse.from(serviceUnit),
                ServiceUnitLocationResponse.from(location));
    }
}
