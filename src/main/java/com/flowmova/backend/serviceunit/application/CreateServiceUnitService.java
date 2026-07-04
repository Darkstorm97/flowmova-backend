package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.serviceunit.api.ServiceUnitResponse;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationType;
import com.flowmova.backend.serviceunitlocation.application.ServiceUnitLocationSlugGenerator;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateServiceUnitService {

    private static final String DEFAULT_LOCATION_NAME = "Principal";

    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;
    private final ServiceUnitLocationSlugGenerator serviceUnitLocationSlugGenerator;

    public CreateServiceUnitService(
            ServiceUnitRepository serviceUnitRepository,
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository,
            ServiceUnitLocationSlugGenerator serviceUnitLocationSlugGenerator) {
        this.serviceUnitRepository = serviceUnitRepository;
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
        this.serviceUnitLocationSlugGenerator = serviceUnitLocationSlugGenerator;
    }

    @Transactional
    public ServiceUnitResponse create(UUID companyId, AuthenticatedUser authenticatedUser, CreateServiceUnitCommand command) {
        Company company = companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        User creator = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        if (command.type() != ServiceUnitType.TICKET_QUEUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service unit type is invalid");
        }

        ServiceUnit serviceUnit = new ServiceUnit(
                company,
                command.name().trim(),
                normalize(command.description()),
                normalize(command.location()),
                null,
                creator);
        serviceUnit.setOneActiveTicketPerUser(command.oneActiveTicketPerUser());
        ServiceUnit savedServiceUnit = serviceUnitRepository.saveAndFlush(serviceUnit);

        ServiceUnitLocation defaultLocation = new ServiceUnitLocation(
                savedServiceUnit,
                DEFAULT_LOCATION_NAME,
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                serviceUnitLocationSlugGenerator.generate(),
                creator);
        ServiceUnitLocation savedDefaultLocation = serviceUnitLocationRepository.saveAndFlush(defaultLocation);

        return ServiceUnitResponse.from(savedServiceUnit, savedDefaultLocation);
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
