package com.flowmova.backend.catalogcategory.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalogcategory.api.CatalogCategoryResponse;
import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
import com.flowmova.backend.company.domain.Company;
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
public class CreateCatalogCategoryService {

    private final CatalogCategoryRepository catalogCategoryRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public CreateCatalogCategoryService(
            CatalogCategoryRepository catalogCategoryRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository) {
        this.catalogCategoryRepository = catalogCategoryRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CatalogCategoryResponse create(
            UUID companyId,
            AuthenticatedUser authenticatedUser,
            CreateCatalogCategoryCommand command) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        User creator = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        String normalizedName = command.name().trim();
        if (catalogCategoryRepository.existsByCompanyIdAndNameIgnoreCase(companyId, normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Catalog category name already exists");
        }

        CatalogCategory category = new CatalogCategory(
                company,
                normalizedName,
                normalizeDescription(command.description()),
                command.displayOrder(),
                creator);

        return CatalogCategoryResponse.from(catalogCategoryRepository.saveAndFlush(category));
    }

    private void requireActiveAdmin(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE || companyUser.getRole() != CompanyRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required");
        }
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
