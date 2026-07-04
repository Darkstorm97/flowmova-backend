package com.flowmova.backend.catalogcategory.application;

import com.flowmova.backend.catalogcategory.api.CatalogCategoryResponse;
import com.flowmova.backend.catalogcategory.domain.CatalogCategoryStatus;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
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

    public ListCatalogCategoriesService(
            CatalogCategoryRepository catalogCategoryRepository,
            CompanyRepository companyRepository) {
        this.catalogCategoryRepository = catalogCategoryRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogCategoryResponse> list(UUID companyId) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        return catalogCategoryRepository.findByCompanyIdAndStatusOrderByDisplayOrderAscNameAsc(
                        companyId,
                        CatalogCategoryStatus.ACTIVE)
                .stream()
                .map(CatalogCategoryResponse::from)
                .toList();
    }
}
