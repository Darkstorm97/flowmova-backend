package com.flowmova.backend.catalogcategory.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalogcategory.application.CreateCatalogCategoryService;
import com.flowmova.backend.catalogcategory.application.ListCatalogCategoriesService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/catalog-categories")
public class CatalogCategoryController {

    private final CreateCatalogCategoryService createCatalogCategoryService;
    private final ListCatalogCategoriesService listCatalogCategoriesService;

    public CatalogCategoryController(
            CreateCatalogCategoryService createCatalogCategoryService,
            ListCatalogCategoriesService listCatalogCategoriesService) {
        this.createCatalogCategoryService = createCatalogCategoryService;
        this.listCatalogCategoriesService = listCatalogCategoriesService;
    }

    @GetMapping
    public List<CatalogCategoryResponse> list(@PathVariable UUID companyId) {
        return listCatalogCategoriesService.list(companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogCategoryResponse create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCatalogCategoryRequest request) {
        return createCatalogCategoryService.create(companyId, authenticatedUser, request.toCommand());
    }
}
