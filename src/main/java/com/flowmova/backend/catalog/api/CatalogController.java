package com.flowmova.backend.catalog.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalog.application.ArchiveCatalogService;
import com.flowmova.backend.catalog.application.CreateCatalogService;
import com.flowmova.backend.catalog.application.ListActiveCatalogsService;
import com.flowmova.backend.catalog.application.UpdateCatalogService;
import com.flowmova.backend.catalog.application.UploadCatalogImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/companies/{companyId}/catalogs")
@Tag(name = "Catalogs", description = "Offres ou elements de catalogue visibles dans une entreprise et utilisables par les articles.")
public class CatalogController {

    private final CreateCatalogService createCatalogService;
    private final ListActiveCatalogsService listActiveCatalogsService;
    private final UpdateCatalogService updateCatalogService;
    private final ArchiveCatalogService archiveCatalogService;
    private final UploadCatalogImageService uploadCatalogImageService;

    public CatalogController(
            CreateCatalogService createCatalogService,
            ListActiveCatalogsService listActiveCatalogsService,
            UpdateCatalogService updateCatalogService,
            ArchiveCatalogService archiveCatalogService,
            UploadCatalogImageService uploadCatalogImageService) {
        this.createCatalogService = createCatalogService;
        this.listActiveCatalogsService = listActiveCatalogsService;
        this.updateCatalogService = updateCatalogService;
        this.archiveCatalogService = archiveCatalogService;
        this.uploadCatalogImageService = uploadCatalogImageService;
    }

    @GetMapping
    @Operation(
            summary = "Lister les catalogues actifs",
            description = "Retourne les catalogues actifs d'une entreprise. Peut etre filtre par categorie avec catalogCategoryId.")
    public List<CatalogResponse> list(
            @PathVariable UUID companyId,
            @RequestParam(required = false) UUID catalogCategoryId) {
        return listActiveCatalogsService.list(companyId, catalogCategoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creer un catalogue",
            description = "Cree un element de catalogue pour une entreprise. Un prix peut etre fourni si l'offre doit contribuer au total indicatif d'un ticket.")
    public CatalogResponse create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCatalogRequest request) {
        return createCatalogService.create(companyId, authenticatedUser, request.toCommand());
    }

    @PutMapping("/{catalogId}")
    @Operation(
            summary = "Modifier un catalogue",
            description = "Met a jour les informations d'un catalogue existant, incluant nom, description, categorie, image et prix optionnel.")
    public CatalogResponse update(
            @PathVariable UUID companyId,
            @PathVariable UUID catalogId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateCatalogRequest request) {
        return updateCatalogService.update(companyId, catalogId, authenticatedUser, request.toCommand());
    }

    @DeleteMapping("/{catalogId}")
    @Operation(
            summary = "Archiver un catalogue",
            description = "Archive un catalogue afin qu'il ne soit plus visible dans les listes publiques actives.")
    public CatalogResponse archive(
            @PathVariable UUID companyId,
            @PathVariable UUID catalogId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return archiveCatalogService.archive(companyId, catalogId, authenticatedUser);
    }

    @PostMapping(path = "/{catalogId}/image", consumes = "multipart/form-data")
    @Operation(
            summary = "Uploader l'image d'un catalogue",
            description = "Remplace l'image d'un element catalogue. Seul un administrateur actif de l'entreprise peut effectuer cette operation.")
    public CatalogResponse uploadImage(
            @PathVariable UUID companyId,
            @PathVariable UUID catalogId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam("image") MultipartFile image) {
        return uploadCatalogImageService.uploadImage(companyId, catalogId, authenticatedUser, image);
    }
}
