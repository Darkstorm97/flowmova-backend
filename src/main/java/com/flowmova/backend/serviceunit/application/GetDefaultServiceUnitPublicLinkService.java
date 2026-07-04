package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.serviceunit.api.ServiceUnitPublicLinkResponse;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationStatus;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GetDefaultServiceUnitPublicLinkService {

    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final String publicBaseUrl;

    public GetDefaultServiceUnitPublicLinkService(
            ServiceUnitRepository serviceUnitRepository,
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            @Value("${flowmova.public-base-url}") String publicBaseUrl) {
        this.serviceUnitRepository = serviceUnitRepository;
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Transactional(readOnly = true)
    public ServiceUnitPublicLinkResponse getDefaultPublicLink(
            UUID companyId,
            UUID serviceUnitId,
            AuthenticatedUser authenticatedUser) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        ServiceUnit serviceUnit = serviceUnitRepository.findByIdAndCompanyId(serviceUnitId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service unit not found"));
        ServiceUnitLocation defaultLocation = serviceUnitLocationRepository
                .findByServiceUnitIdAndDefaultLocationTrueAndStatus(serviceUnit.getId(), ServiceUnitLocationStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Default location not found"));

        return new ServiceUnitPublicLinkResponse(
                serviceUnit.getId(),
                defaultLocation.getId(),
                defaultLocation.getPublicAccessSlug(),
                buildPublicUrl(defaultLocation.getPublicAccessSlug()));
    }

    private void requireActiveAdmin(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE || companyUser.getRole() != CompanyRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required");
        }
    }

    private String buildPublicUrl(String publicAccessSlug) {
        String normalizedBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        return "%s/public/locations/%s".formatted(normalizedBaseUrl, publicAccessSlug);
    }
}
