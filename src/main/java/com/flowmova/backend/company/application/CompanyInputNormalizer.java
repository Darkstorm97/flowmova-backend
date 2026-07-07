package com.flowmova.backend.company.application;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyBusinessType;
import com.flowmova.backend.company.domain.CompanyOperationalStatus;
import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class CompanyInputNormalizer {

    String normalizeDescription(String description) {
        return normalizeOptionalText(description);
    }

    String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    String normalizeCountry(String country) {
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

    String normalizeCurrency(String currency) {
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

    CompanyBusinessType normalizeBusinessType(String businessType) {
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

    CompanyOperationalStatus normalizeOperationalStatus(
            String operationalStatus,
            CompanyOperationalStatus fallbackStatus) {
        if (operationalStatus == null || operationalStatus.isBlank()) {
            return fallbackStatus == null ? Company.DEFAULT_OPERATIONAL_STATUS : fallbackStatus;
        }

        String normalizedOperationalStatus = operationalStatus.trim().toUpperCase(Locale.ROOT);
        try {
            return CompanyOperationalStatus.valueOf(normalizedOperationalStatus);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operational status must be OPEN or CLOSED");
        }
    }
}
