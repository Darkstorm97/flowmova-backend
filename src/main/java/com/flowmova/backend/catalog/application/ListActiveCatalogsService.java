package com.flowmova.backend.catalog.application;

import com.flowmova.backend.catalog.api.CatalogResponse;
import com.flowmova.backend.catalog.domain.CatalogStatus;
import com.flowmova.backend.catalog.infrastructure.CatalogRepository;
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
public class ListActiveCatalogsService {

    private final CatalogRepository catalogRepository;
    private final CatalogCategoryRepository catalogCategoryRepository;
    private final CompanyRepository companyRepository;

    public ListActiveCatalogsService(
            CatalogRepository catalogRepository,
            CatalogCategoryRepository catalogCategoryRepository,
            CompanyRepository companyRepository) {
        this.catalogRepository = catalogRepository;
        this.catalogCategoryRepository = catalogCategoryRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogResponse> list(UUID companyId, UUID catalogCategoryId) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        if (catalogCategoryId != null) {
            catalogCategoryRepository.findByIdAndCompanyId(catalogCategoryId, companyId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Catalog category is invalid"));

            return catalogRepository.findByCompanyIdAndCatalogCategoryIdAndStatusOrderByNameAsc(
                            companyId,
                            catalogCategoryId,
                            CatalogStatus.ACTIVE)
                    .stream()
                    .map(CatalogResponse::from)
                    .toList();
        }

        return catalogRepository.findByCompanyIdAndStatusOrderByCatalogCategoryDisplayOrderAscCatalogCategoryNameAscNameAsc(
                        companyId,
                        CatalogStatus.ACTIVE)
                .stream()
                .map(CatalogResponse::from)
                .toList();
    }
}
