package com.flowmova.backend.company.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.api.CompanyResponse;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UpdateCompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;
    private final CompanyInputNormalizer normalizer;

    public UpdateCompanyService(
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository,
            CompanyInputNormalizer normalizer) {
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
        this.normalizer = normalizer;
    }

    @Transactional
    public CompanyResponse update(
            UUID companyId,
            AuthenticatedUser authenticatedUser,
            UpdateCompanyCommand command) {
        Company company = companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        User updater = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        company.update(
                command.name().trim(),
                normalizer.normalizeDescription(command.description()),
                normalizer.normalizeOptionalText(command.addressLine1()),
                normalizer.normalizeOptionalText(command.addressLine2()),
                normalizer.normalizeOptionalText(command.city()),
                normalizer.normalizeOptionalText(command.region()),
                normalizer.normalizeOptionalText(command.postalCode()),
                normalizer.normalizeCountry(command.country()),
                command.latitude(),
                command.longitude(),
                normalizer.normalizeCurrency(command.currency()),
                normalizer.normalizeBusinessType(command.businessType()),
                updater);

        return CompanyResponse.from(companyRepository.saveAndFlush(company));
    }

    private void requireActiveAdmin(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE || companyUser.getRole() != CompanyRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required");
        }
    }
}
