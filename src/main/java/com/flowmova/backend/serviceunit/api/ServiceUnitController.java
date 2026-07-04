package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.serviceunit.application.CreateServiceUnitService;
import com.flowmova.backend.serviceunit.application.GetDefaultServiceUnitPublicLinkService;
import com.flowmova.backend.serviceunit.application.GetOpenServiceUnitService;
import com.flowmova.backend.serviceunit.application.ListOpenServiceUnitsService;
import com.flowmova.backend.serviceunit.application.OpenServiceUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/companies/{companyId}/service-units")
@Tag(name = "Service Units", description = "Unites de service publiques et operations de creation/ouverture par administrateur.")
public class ServiceUnitController {

    private final CreateServiceUnitService createServiceUnitService;
    private final GetDefaultServiceUnitPublicLinkService getDefaultServiceUnitPublicLinkService;
    private final OpenServiceUnitService openServiceUnitService;
    private final ListOpenServiceUnitsService listOpenServiceUnitsService;
    private final GetOpenServiceUnitService getOpenServiceUnitService;

    public ServiceUnitController(
            CreateServiceUnitService createServiceUnitService,
            GetDefaultServiceUnitPublicLinkService getDefaultServiceUnitPublicLinkService,
            OpenServiceUnitService openServiceUnitService,
            ListOpenServiceUnitsService listOpenServiceUnitsService,
            GetOpenServiceUnitService getOpenServiceUnitService) {
        this.createServiceUnitService = createServiceUnitService;
        this.getDefaultServiceUnitPublicLinkService = getDefaultServiceUnitPublicLinkService;
        this.openServiceUnitService = openServiceUnitService;
        this.listOpenServiceUnitsService = listOpenServiceUnitsService;
        this.getOpenServiceUnitService = getOpenServiceUnitService;
    }

    @GetMapping
    @Operation(
            summary = "Lister les unites de service ouvertes",
            description = "Retourne les unites de service ouvertes d'une entreprise active. Endpoint public pour la navigation utilisateur.")
    public List<ServiceUnitResponse> listOpen(@PathVariable UUID companyId) {
        return listOpenServiceUnitsService.list(companyId);
    }

    @GetMapping("/{serviceUnitId}")
    @Operation(
            summary = "Consulter une unite de service ouverte",
            description = "Retourne les details publics d'une unite de service ouverte, incluant ses emplacements et articles disponibles.")
    public PublicServiceUnitDetailResponse getOpen(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId) {
        return getOpenServiceUnitService.get(companyId, serviceUnitId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creer une unite de service",
            description = "Cree une unite de service dans une entreprise. Un emplacement par defaut est cree pour le lien public/QR code.")
    public ServiceUnitResponse create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateServiceUnitRequest request) {
        return createServiceUnitService.create(companyId, authenticatedUser, request.toCommand());
    }

    @GetMapping("/{serviceUnitId}/default-public-link")
    @Operation(
            summary = "Consulter le lien public par defaut",
            description = "Retourne l'URL publique de l'emplacement par defaut d'une unite de service. JWT administrateur requis.")
    public ServiceUnitPublicLinkResponse getDefaultPublicLink(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return getDefaultServiceUnitPublicLinkService.getDefaultPublicLink(companyId, serviceUnitId, authenticatedUser);
    }

    @PostMapping("/{serviceUnitId}/open")
    @Operation(
            summary = "Ouvrir une unite de service",
            description = "Passe une unite de service en statut ouvert afin de la rendre visible publiquement.")
    public ServiceUnitResponse open(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return openServiceUnitService.open(companyId, serviceUnitId, authenticatedUser);
    }
}
