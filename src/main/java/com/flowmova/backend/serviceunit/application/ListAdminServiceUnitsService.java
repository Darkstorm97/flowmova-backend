package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.serviceunit.api.ServiceUnitResponse;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListAdminServiceUnitsService {

    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;

    public ListAdminServiceUnitsService(
            ServiceUnitRepository serviceUnitRepository,
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository) {
        this.serviceUnitRepository = serviceUnitRepository;
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
    }

    @Transactional(readOnly = true)
    public Page<ServiceUnitResponse> list(
            UUID companyId,
            AuthenticatedUser authenticatedUser,
            ServiceUnitStatus status,
            Pageable pageable) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        PageRequest normalizedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                normalizeSort(pageable.getSort()));

        Page<ServiceUnit> serviceUnits = status == null
                ? serviceUnitRepository.findByCompanyId(companyId, normalizedPageable)
                : serviceUnitRepository.findByCompanyIdAndStatus(companyId, status, normalizedPageable);

        return serviceUnits.map(serviceUnit -> ServiceUnitResponse.from(
                serviceUnit,
                serviceUnitLocationRepository
                        .findByServiceUnitIdAndDefaultLocationTrueAndStatus(
                                serviceUnit.getId(),
                                ServiceUnitLocationStatus.ACTIVE)
                        .orElse(null)));
    }

    private void requireActiveAdmin(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE || companyUser.getRole() != CompanyRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required");
        }
    }

    private Sort normalizeSort(Sort requestedSort) {
        if (requestedSort.isUnsorted()) {
            return Sort.by(Sort.Direction.ASC, "name");
        }

        return Sort.by(requestedSort.stream()
                .map(order -> new Sort.Order(order.getDirection(), sortProperty(order.getProperty())))
                .toList());
    }

    private String sortProperty(String requestedProperty) {
        return switch (requestedProperty) {
            case "name" -> "name";
            case "createdAt" -> "createdAt";
            case "updatedAt" -> "updatedAt";
            case "status" -> "status";
            default -> "name";
        };
    }
}
