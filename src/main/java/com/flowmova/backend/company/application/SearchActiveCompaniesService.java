package com.flowmova.backend.company.application;

import com.flowmova.backend.company.api.CompanyResponse;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchActiveCompaniesService {

    private final CompanyRepository companyRepository;

    public SearchActiveCompaniesService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public Page<CompanyResponse> search(String query, Pageable pageable) {
        Pageable normalizedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                normalizeSort(pageable.getSort()));

        if (query == null || query.isBlank()) {
            return companyRepository.findByStatus(CompanyStatus.ACTIVE, normalizedPageable)
                    .map(CompanyResponse::from);
        }

        return companyRepository
                .findByStatusAndNameContainingIgnoreCase(CompanyStatus.ACTIVE, query.trim(), normalizedPageable)
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
            case "createdAt", "status" -> requestedProperty;
            default -> "name";
        };
    }
}
