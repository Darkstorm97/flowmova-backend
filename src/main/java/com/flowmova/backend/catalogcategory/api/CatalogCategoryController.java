package com.flowmova.backend.catalogcategory.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalogcategory.application.ArchiveCatalogCategoryService;
import com.flowmova.backend.catalogcategory.application.CreateCatalogCategoryService;
import com.flowmova.backend.catalogcategory.application.ListCatalogCategoriesService;
import com.flowmova.backend.catalogcategory.application.UpdateCatalogCategoryService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/catalog-categories")
@Tag(name = "Catalog Categories", description = "Categories permettant de classer les catalogues d'une entreprise.")
public class CatalogCategoryController {

    private final CreateCatalogCategoryService createCatalogCategoryService;
    private final ListCatalogCategoriesService listCatalogCategoriesService;
    private final UpdateCatalogCategoryService updateCatalogCategoryService;
    private final ArchiveCatalogCategoryService archiveCatalogCategoryService;

    public CatalogCategoryController(
            CreateCatalogCategoryService createCatalogCategoryService,
            ListCatalogCategoriesService listCatalogCategoriesService,
            UpdateCatalogCategoryService updateCatalogCategoryService,
            ArchiveCatalogCategoryService archiveCatalogCategoryService) {
        this.createCatalogCategoryService = createCatalogCategoryService;
        this.listCatalogCategoriesService = listCatalogCategoriesService;
        this.updateCatalogCategoryService = updateCatalogCategoryService;
        this.archiveCatalogCategoryService = archiveCatalogCategoryService;
    }

    @GetMapping
    @Operation(
            summary = "Lister les categories de catalogue",
            description = "Retourne les categories actives d'une entreprise. Endpoint public utilise dans le parcours de consultation.")
    public List<CatalogCategoryResponse> list(@PathVariable UUID companyId) {
        return listCatalogCategoriesService.list(companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creer une categorie de catalogue",
            description = "Cree une categorie de catalogue pour une entreprise. JWT requis et role administrateur requis.")
    public CatalogCategoryResponse create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCatalogCategoryRequest request) {
        return createCatalogCategoryService.create(companyId, authenticatedUser, request.toCommand());
    }

    @PutMapping("/{categoryId}")
    @Operation(
            summary = "Modifier une categorie de catalogue",
            description = "Met a jour le nom, la description et l'ordre d'affichage d'une categorie. JWT requis et role administrateur requis.")
    public CatalogCategoryResponse update(
            @PathVariable UUID companyId,
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateCatalogCategoryRequest request) {
        return updateCatalogCategoryService.update(companyId, categoryId, authenticatedUser, request.toCommand());
    }

    @DeleteMapping("/{categoryId}")
    @Operation(
            summary = "Archiver une categorie de catalogue",
            description = "Archive une categorie afin qu'elle ne soit plus visible dans les listes actives.")
    public CatalogCategoryResponse archive(
            @PathVariable UUID companyId,
            @PathVariable UUID categoryId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return archiveCatalogCategoryService.archive(companyId, categoryId, authenticatedUser);
    }
}
