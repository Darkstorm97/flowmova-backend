package com.flowmova.backend.serviceunitlocation.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.serviceunit.api.ServiceUnitLocationResponse;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationType;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateServiceUnitLocationService {

    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final UserRepository userRepository;
    private final ServiceUnitLocationSlugGenerator serviceUnitLocationSlugGenerator;
    private final ServiceUnitLocationPublicUrlBuilder serviceUnitLocationPublicUrlBuilder;

    public CreateServiceUnitLocationService(
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            ServiceUnitRepository serviceUnitRepository,
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            UserRepository userRepository,
            ServiceUnitLocationSlugGenerator serviceUnitLocationSlugGenerator,
            ServiceUnitLocationPublicUrlBuilder serviceUnitLocationPublicUrlBuilder) {
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.serviceUnitRepository = serviceUnitRepository;
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.userRepository = userRepository;
        this.serviceUnitLocationSlugGenerator = serviceUnitLocationSlugGenerator;
        this.serviceUnitLocationPublicUrlBuilder = serviceUnitLocationPublicUrlBuilder;
    }

    @Transactional
    public ServiceUnitLocationResponse create(
            UUID companyId,
            UUID serviceUnitId,
            AuthenticatedUser authenticatedUser,
            CreateServiceUnitLocationCommand command) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        User creator = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        ServiceUnit serviceUnit = serviceUnitRepository.findByIdAndCompanyId(serviceUnitId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service unit not found"));

        ServiceUnitLocation location = new ServiceUnitLocation(
                serviceUnit,
                command.name().trim(),
                normalize(command.description()),
                ServiceUnitLocationType.CUSTOM,
                false,
                serviceUnitLocationSlugGenerator.generate(),
                creator);
        ServiceUnitLocation savedLocation = serviceUnitLocationRepository.saveAndFlush(location);

        return ServiceUnitLocationResponse.from(
                savedLocation,
                serviceUnitLocationPublicUrlBuilder.build(savedLocation.getPublicAccessSlug()));
    }

    private void requireActiveAdmin(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE || companyUser.getRole() != CompanyRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
