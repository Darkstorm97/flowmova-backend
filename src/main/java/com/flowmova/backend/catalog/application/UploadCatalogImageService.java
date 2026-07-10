package com.flowmova.backend.catalog.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalog.api.CatalogResponse;
import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.infrastructure.CatalogRepository;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.shared.storage.FileStorageService;
import com.flowmova.backend.shared.storage.StoredFile;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UploadCatalogImageService {

    private final CatalogRepository catalogRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public UploadCatalogImageService(
            CatalogRepository catalogRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService) {
        this.catalogRepository = catalogRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public CatalogResponse uploadImage(
            UUID companyId,
            UUID catalogId,
            AuthenticatedUser authenticatedUser,
            MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        User updater = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        Catalog catalog = catalogRepository.findByIdAndCompanyId(catalogId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Catalog not found"));

        try {
            StoredFile storedFile = fileStorageService.storeImage(
                    "catalogs/%s/%s".formatted(companyId, catalogId),
                    image.getOriginalFilename(),
                    image.getContentType(),
                    image.getSize(),
                    image.getInputStream());
            catalog.updateImageUrl(storedFile.publicUrl(), updater);
            return CatalogResponse.from(catalogRepository.saveAndFlush(catalog));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store catalog image", exception);
        }
    }

    private void requireActiveAdmin(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE || companyUser.getRole() != CompanyRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required");
        }
    }
}
