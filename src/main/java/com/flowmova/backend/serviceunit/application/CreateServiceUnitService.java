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
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateServiceUnitService {

    private static final String DEFAULT_LOCATION_NAME = "Principal";
    private static final char[] SLUG_ALPHABET = "23456789abcdefghjkmnpqrstuvwxyz".toCharArray();
    private static final int SLUG_RANDOM_LENGTH = 12;

    private final ServiceUnitRepository serviceUnitRepository;
    private final ServiceUnitLocationRepository serviceUnitLocationRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public CreateServiceUnitService(
            ServiceUnitRepository serviceUnitRepository,
            ServiceUnitLocationRepository serviceUnitLocationRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository) {
        this.serviceUnitRepository = serviceUnitRepository;
        this.serviceUnitLocationRepository = serviceUnitLocationRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
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
        ServiceUnit savedServiceUnit = serviceUnitRepository.saveAndFlush(serviceUnit);

        ServiceUnitLocation defaultLocation = new ServiceUnitLocation(
                savedServiceUnit,
                DEFAULT_LOCATION_NAME,
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                generatePublicAccessSlug(),
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

    private String generatePublicAccessSlug() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String slug = "loc-" + randomSlugPart();
            if (serviceUnitLocationRepository.findByPublicAccessSlug(slug).isEmpty()) {
                return slug;
            }
        }

        return "loc-" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    private String randomSlugPart() {
        StringBuilder slug = new StringBuilder(SLUG_RANDOM_LENGTH);
        for (int index = 0; index < SLUG_RANDOM_LENGTH; index++) {
            slug.append(SLUG_ALPHABET[secureRandom.nextInt(SLUG_ALPHABET.length)]);
        }
        return slug.toString();
    }
}
