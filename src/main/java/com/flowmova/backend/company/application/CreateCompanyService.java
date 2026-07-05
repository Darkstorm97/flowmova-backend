package com.flowmova.backend.company.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyBusinessType;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateCompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public CreateCompanyService(
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Company createCompany(AuthenticatedUser authenticatedUser, CreateCompanyCommand command) {
        User creator = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        Company company = new Company(
                command.name().trim(),
                normalizeDescription(command.description()),
                normalizeOptionalText(command.addressLine1()),
                normalizeOptionalText(command.addressLine2()),
                normalizeOptionalText(command.city()),
                normalizeOptionalText(command.region()),
                normalizeOptionalText(command.postalCode()),
                normalizeCountry(command.country()),
                command.latitude(),
                command.longitude(),
                normalizeCurrency(command.currency()),
                normalizeBusinessType(command.businessType()),
                creator);
        company.activate();

        Company savedCompany = companyRepository.save(company);
        companyUserRepository.save(new CompanyUser(savedCompany.getId(), creator, CompanyRole.ADMIN));

        return savedCompany;
    }

    private String normalizeDescription(String description) {
        return normalizeOptionalText(description);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizeCountry(String country) {
        String normalizedCountry = normalizeOptionalText(country);
        if (normalizedCountry == null) {
            return null;
        }

        String upperCaseCountry = normalizedCountry.toUpperCase(Locale.ROOT);
        if (Arrays.stream(Locale.getISOCountries()).noneMatch(upperCaseCountry::equals)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Country must be a valid ISO 3166-1 alpha-2 code");
        }

        return upperCaseCountry;
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return Company.DEFAULT_CURRENCY;
        }

        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        if (normalizedCurrency.length() != 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be a 3-letter ISO 4217 code");
        }

        try {
            Currency.getInstance(normalizedCurrency);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be a valid ISO 4217 code");
        }

        return normalizedCurrency;
    }

    private CompanyBusinessType normalizeBusinessType(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return Company.DEFAULT_BUSINESS_TYPE;
        }

        String normalizedBusinessType = businessType.trim().toUpperCase(Locale.ROOT);
        try {
            return CompanyBusinessType.valueOf(normalizedBusinessType);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business type must be a supported company business type");
        }
    }
}
