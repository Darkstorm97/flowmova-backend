package com.flowmova.backend.company.application;

import com.flowmova.backend.company.api.CompanyResponse;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyBusinessType;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SearchActiveCompaniesService {

    private final CompanyRepository companyRepository;

    public SearchActiveCompaniesService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> search(String query, Pageable pageable) {
        return search(new SearchCompaniesQuery(query, null, null, null, null), pageable);
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> search(SearchCompaniesQuery query, Pageable pageable) {
        Pageable normalizedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                normalizeSort(pageable.getSort()));

        return companyRepository.findAll(toSpecification(query), normalizedPageable)
                .map(CompanyResponse::from);
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
            case "businessType", "city", "country", "createdAt", "region", "status" -> requestedProperty;
            default -> "name";
        };
    }

    private Specification<Company> toSpecification(SearchCompaniesQuery query) {
        String text = normalizeOptionalText(query.text());
        CompanyBusinessType businessType = normalizeBusinessType(query.businessType());
        String city = normalizeOptionalText(query.city());
        String region = normalizeOptionalText(query.region());
        String country = normalizeCountry(query.country());

        return (root, criteriaQuery, criteriaBuilder) -> {
            var predicate = criteriaBuilder.equal(root.get("status"), CompanyStatus.ACTIVE);

            if (text != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.<String>get("name")),
                                "%" + text.toLowerCase(Locale.ROOT) + "%"));
            }

            if (businessType != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("businessType"), businessType));
            }

            if (city != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.equal(criteriaBuilder.lower(root.<String>get("city")), city.toLowerCase(Locale.ROOT)));
            }

            if (region != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.equal(criteriaBuilder.lower(root.<String>get("region")), region.toLowerCase(Locale.ROOT)));
            }

            if (country != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("country"), country));
            }

            return predicate;
        };
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

    private CompanyBusinessType normalizeBusinessType(String businessType) {
        if (businessType == null || businessType.isBlank()) {
            return null;
        }

        String normalizedBusinessType = businessType.trim().toUpperCase(Locale.ROOT);
        try {
            return CompanyBusinessType.valueOf(normalizedBusinessType);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business type must be a supported company business type");
        }
    }
}
