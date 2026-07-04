package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.serviceunit.api.ServiceUnitResponse;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GetOpenServiceUnitService {

    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final CompanyRepository companyRepository;

    public GetOpenServiceUnitService(
            ServiceUnitRepository serviceUnitRepository,
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            CompanyRepository companyRepository) {
        this.serviceUnitRepository = serviceUnitRepository;
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public ServiceUnitResponse get(UUID companyId, UUID serviceUnitId) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        ServiceUnit serviceUnit = serviceUnitRepository
                .findByIdAndCompanyIdAndStatus(serviceUnitId, companyId, ServiceUnitStatus.OPEN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service unit not found"));

        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository
                .findByServiceUnitIdAndDefaultLocationTrueAndStatus(serviceUnit.getId(), ServiceUnitLocationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default location not found"));

        return ServiceUnitResponse.from(serviceUnit, defaultLocation);
    }
}
