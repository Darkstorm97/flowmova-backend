package com.flowmova.backend.catalog.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalog.api.CatalogResponse;
import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.infrastructure.CatalogRepository;
import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
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
public class CreateCatalogService {

    private final CatalogRepository catalogRepository;
    private final CatalogCategoryRepository catalogCategoryRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public CreateCatalogService(
            CatalogRepository catalogRepository,
            CatalogCategoryRepository catalogCategoryRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository) {
        this.catalogRepository = catalogRepository;
        this.catalogCategoryRepository = catalogCategoryRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CatalogResponse create(UUID companyId, AuthenticatedUser authenticatedUser, CreateCatalogCommand command) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        if (company.getStatus() != CompanyStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found");
        }

        User creator = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        CatalogCategory category = catalogCategoryRepository.findByIdAndCompanyId(command.catalogCategoryId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catalog category is invalid"));

        Catalog catalog = new Catalog(
                company,
                category,
                command.name().trim(),
                normalize(command.description()),
                normalize(command.imageUrl()),
                command.priceAmount(),
                creator);

        return CatalogResponse.from(catalogRepository.saveAndFlush(catalog));
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
}
