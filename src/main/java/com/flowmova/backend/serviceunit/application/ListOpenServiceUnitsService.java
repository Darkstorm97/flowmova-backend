package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.serviceunit.api.ServiceUnitResponse;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListOpenServiceUnitsService {

    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final CompanyRepository companyRepository;

    public ListOpenServiceUnitsService(
            ServiceUnitRepository serviceUnitRepository,
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            CompanyRepository companyRepository) {
        this.serviceUnitRepository = serviceUnitRepository;
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceUnitResponse> list(UUID companyId) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        return serviceUnitRepository.findByCompanyIdAndStatusOrderByNameAsc(companyId, ServiceUnitStatus.OPEN)
                .stream()
                .map(serviceUnit -> serviceUnitLocationRepository
                        .findByServiceUnitIdAndDefaultLocationTrueAndStatus(
                                serviceUnit.getId(),
                                ServiceUnitLocationStatus.ACTIVE)
                        .map(defaultLocation -> ServiceUnitResponse.from(serviceUnit, defaultLocation)))
                .flatMap(optionalResponse -> optionalResponse.stream())
                .toList();
    }
}
