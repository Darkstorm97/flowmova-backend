package com.flowmova.backend.catalog.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalog.application.CreateCatalogService;
import com.flowmova.backend.catalog.application.ListActiveCatalogsService;
import com.flowmova.backend.catalog.application.UpdateCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/catalogs")
public class CatalogController {

    private final CreateCatalogService createCatalogService;
    private final ListActiveCatalogsService listActiveCatalogsService;
    private final UpdateCatalogService updateCatalogService;

    public CatalogController(
            CreateCatalogService createCatalogService,
            ListActiveCatalogsService listActiveCatalogsService,
            UpdateCatalogService updateCatalogService) {
        this.createCatalogService = createCatalogService;
        this.listActiveCatalogsService = listActiveCatalogsService;
        this.updateCatalogService = updateCatalogService;
    }

    @GetMapping
    public List<CatalogResponse> list(
            @PathVariable UUID companyId,
            @RequestParam(required = false) UUID catalogCategoryId) {
        return listActiveCatalogsService.list(companyId, catalogCategoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogResponse create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCatalogRequest request) {
        return createCatalogService.create(companyId, authenticatedUser, request.toCommand());
    }

    @PutMapping("/{catalogId}")
    public CatalogResponse update(
            @PathVariable UUID companyId,
            @PathVariable UUID catalogId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateCatalogRequest request) {
        return updateCatalogService.update(companyId, catalogId, authenticatedUser, request.toCommand());
    }
}
