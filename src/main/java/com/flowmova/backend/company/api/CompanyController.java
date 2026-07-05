package com.flowmova.backend.company.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.application.CreateCompanyService;
import com.flowmova.backend.company.application.GetActiveCompanyService;
import com.flowmova.backend.company.application.SearchActiveCompaniesService;
import com.flowmova.backend.company.application.SearchCompaniesQuery;
import com.flowmova.backend.shared.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
@Tag(name = "Companies", description = "Recherche publique des entreprises et creation d'une entreprise par un utilisateur authentifie.")
public class CompanyController {

    private final CreateCompanyService createCompanyService;
    private final SearchActiveCompaniesService searchActiveCompaniesService;
    private final GetActiveCompanyService getActiveCompanyService;

    public CompanyController(
            CreateCompanyService createCompanyService,
            SearchActiveCompaniesService searchActiveCompaniesService,
            GetActiveCompanyService getActiveCompanyService) {
        this.createCompanyService = createCompanyService;
        this.searchActiveCompaniesService = searchActiveCompaniesService;
        this.getActiveCompanyService = getActiveCompanyService;
    }

    @GetMapping
    @Operation(
            summary = "Rechercher les entreprises actives",
            description = "Retourne les entreprises actives visibles publiquement. Le parametre q filtre par recherche texte. Les filtres optionnels businessType, city, region et country peuvent etre combines.")
    public PageResponse<CompanyResponse> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String country,
            Pageable pageable) {
        return PageResponse.from(searchActiveCompaniesService.search(
                new SearchCompaniesQuery(query, businessType, city, region, country),
                pageable));
    }

    @GetMapping("/{companyId}")
    @Operation(
            summary = "Consulter une entreprise active",
            description = "Retourne la fiche publique d'une entreprise active a partir de son identifiant.")
    public CompanyResponse get(@PathVariable UUID companyId) {
        return getActiveCompanyService.getActiveCompany(companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creer une entreprise",
            description = "Cree une entreprise et rattache l'utilisateur authentifie comme administrateur.")
    public CompanyResponse create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCompanyRequest request) {
        return CompanyResponse.from(createCompanyService.createCompany(authenticatedUser, request.toCommand()));
    }
}
