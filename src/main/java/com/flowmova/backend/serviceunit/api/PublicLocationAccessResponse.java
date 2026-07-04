package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.company.api.CompanyResponse;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.item.api.ItemResponse;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import java.util.List;

public record PublicLocationAccessResponse(
        CompanyResponse company,
        PublicServiceUnitResponse serviceUnit,
        ServiceUnitLocationResponse location,
        List<ItemResponse> items) {

    public static PublicLocationAccessResponse from(
            Company company,
            ServiceUnit serviceUnit,
            ServiceUnitLocation location,
            List<Item> items) {
        return new PublicLocationAccessResponse(
                CompanyResponse.from(company),
                PublicServiceUnitResponse.from(serviceUnit),
                ServiceUnitLocationResponse.from(location),
                items.stream()
                        .map(ItemResponse::from)
                        .toList());
    }
}
