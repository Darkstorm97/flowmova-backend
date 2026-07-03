package com.flowmova.backend.catalogcategory.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalogcategory.api.CatalogCategoryResponse;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListCatalogCategoriesService {

    private final CatalogCategoryRepository catalogCategoryRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;

    public ListCatalogCategoriesService(
            CatalogCategoryRepository catalogCategoryRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository) {
        this.catalogCategoryRepository = catalogCategoryRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogCategoryResponse> list(UUID companyId, AuthenticatedUser authenticatedUser) {
        if (!companyRepository.existsById(companyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found");
        }

        requireActiveCompanyMember(companyId, authenticatedUser.userId());

        return catalogCategoryRepository.findByCompanyIdOrderByDisplayOrderAscNameAsc(companyId).stream()
                .map(CatalogCategoryResponse::from)
                .toList();
    }

    private void requireActiveCompanyMember(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Company membership is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company membership is required");
        }
    }
}
