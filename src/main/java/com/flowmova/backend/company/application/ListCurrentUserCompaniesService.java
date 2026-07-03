package com.flowmova.backend.company.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.api.CurrentUserCompanyResponse;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCurrentUserCompaniesService {

    private final CompanyUserRepository companyUserRepository;

    public ListCurrentUserCompaniesService(CompanyUserRepository companyUserRepository) {
        this.companyUserRepository = companyUserRepository;
    }

    @Transactional(readOnly = true)
    public Page<CurrentUserCompanyResponse> listCompanies(AuthenticatedUser authenticatedUser, Pageable pageable) {
        Pageable normalizedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                normalizeSort(pageable.getSort()));

        return companyUserRepository
                .findByUserIdAndStatus(authenticatedUser.userId(), CompanyUserStatus.ACTIVE, normalizedPageable)
                .map(CurrentUserCompanyResponse::from);
    }

    private Sort normalizeSort(Sort requestedSort) {
        if (requestedSort.isUnsorted()) {
            return Sort.by(Sort.Direction.ASC, "company.name");
        }

        return Sort.by(requestedSort.stream()
                .map(order -> new Sort.Order(order.getDirection(), sortProperty(order.getProperty())))
                .toList());
    }

    private String sortProperty(String requestedProperty) {
        return switch (requestedProperty) {
            case "name" -> "company.name";
            case "createdAt" -> "company.createdAt";
            case "status" -> "company.status";
            case "role" -> "role";
            default -> "company.name";
        };
    }
}
