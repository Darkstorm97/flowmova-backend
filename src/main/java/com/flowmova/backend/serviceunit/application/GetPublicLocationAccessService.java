package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.catalog.domain.CatalogStatus;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.domain.ItemStatus;
import com.flowmova.backend.item.infrastructure.ItemRepository;
import com.flowmova.backend.serviceunit.api.PublicLocationAccessResponse;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GetPublicLocationAccessService {

    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final ItemRepository itemRepository;

    public GetPublicLocationAccessService(
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            ItemRepository itemRepository) {
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public PublicLocationAccessResponse get(String publicAccessSlug) {
        ServiceUnitLocation location = serviceUnitLocationRepository
                .findByPublicAccessSlugAndStatus(publicAccessSlug, ServiceUnitLocationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public location not found"));

        ServiceUnit serviceUnit = location.getServiceUnit();
        if (serviceUnit.getStatus() != ServiceUnitStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Public location not found");
        }

        Company company = serviceUnit.getCompany();
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Public location not found");
        }

        List<Item> items = itemRepository
                .findByServiceUnitIdAndStatusAndAvailabilityAndCatalogStatusOrderByDisplayOrderAscCatalogNameAsc(
                        serviceUnit.getId(),
                        ItemStatus.ACTIVE,
                        ItemAvailability.AVAILABLE,
                        CatalogStatus.ACTIVE);

        return PublicLocationAccessResponse.from(company, serviceUnit, location, items);
    }
}
