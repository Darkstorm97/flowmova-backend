package com.flowmova.backend.catalog.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalog.application.CreateCatalogService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/catalogs")
public class CatalogController {

    private final CreateCatalogService createCatalogService;

    public CatalogController(CreateCatalogService createCatalogService) {
        this.createCatalogService = createCatalogService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogResponse create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCatalogRequest request) {
        return createCatalogService.create(companyId, authenticatedUser, request.toCommand());
    }
}
