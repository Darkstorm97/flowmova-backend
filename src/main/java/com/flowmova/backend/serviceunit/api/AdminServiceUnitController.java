package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.item.api.CreateItemRequest;
import com.flowmova.backend.item.api.ItemResponse;
import com.flowmova.backend.item.api.UpdateItemRequest;
import com.flowmova.backend.item.application.CreateItemService;
import com.flowmova.backend.item.application.ListServiceUnitItemsService;
import com.flowmova.backend.item.application.UpdateItemService;
import com.flowmova.backend.serviceunit.application.ArchiveServiceUnitService;
import com.flowmova.backend.serviceunit.application.CloseServiceUnitService;
import com.flowmova.backend.serviceunitlocation.application.CreateServiceUnitLocationService;
import com.flowmova.backend.serviceunitlocation.application.ListServiceUnitLocationsService;
import com.flowmova.backend.serviceunit.application.ListAdminServiceUnitsService;
import com.flowmova.backend.serviceunit.application.UpdateServiceUnitService;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.shared.api.PageResponse;
import com.flowmova.backend.ticket.api.ChangeTicketStatusRequest;
import com.flowmova.backend.ticket.api.TicketResponse;
import com.flowmova.backend.ticket.application.ChangeTicketStatusService;
import com.flowmova.backend.ticket.application.ListServiceUnitTicketsService;
import com.flowmova.backend.ticket.domain.TicketStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/admin/service-units")
@Tag(name = "Admin Service Units", description = "Administration des unites de service, emplacements, articles et tickets d'une entreprise.")
public class AdminServiceUnitController {

    private final ListAdminServiceUnitsService listAdminServiceUnitsService;
    private final UpdateServiceUnitService updateServiceUnitService;
    private final CreateItemService createItemService;
    private final ListServiceUnitItemsService listServiceUnitItemsService;
    private final UpdateItemService updateItemService;
    private final ListServiceUnitTicketsService listServiceUnitTicketsService;
    private final ChangeTicketStatusService changeTicketStatusService;
    private final CreateServiceUnitLocationService createServiceUnitLocationService;
    private final ListServiceUnitLocationsService listServiceUnitLocationsService;
    private final CloseServiceUnitService closeServiceUnitService;
    private final ArchiveServiceUnitService archiveServiceUnitService;

    public AdminServiceUnitController(
            ListAdminServiceUnitsService listAdminServiceUnitsService,
            UpdateServiceUnitService updateServiceUnitService,
            CreateItemService createItemService,
            ListServiceUnitItemsService listServiceUnitItemsService,
            UpdateItemService updateItemService,
            ListServiceUnitTicketsService listServiceUnitTicketsService,
            ChangeTicketStatusService changeTicketStatusService,
            CreateServiceUnitLocationService createServiceUnitLocationService,
            ListServiceUnitLocationsService listServiceUnitLocationsService,
            CloseServiceUnitService closeServiceUnitService,
            ArchiveServiceUnitService archiveServiceUnitService) {
        this.listAdminServiceUnitsService = listAdminServiceUnitsService;
        this.updateServiceUnitService = updateServiceUnitService;
        this.createItemService = createItemService;
        this.listServiceUnitItemsService = listServiceUnitItemsService;
        this.updateItemService = updateItemService;
        this.listServiceUnitTicketsService = listServiceUnitTicketsService;
        this.changeTicketStatusService = changeTicketStatusService;
        this.createServiceUnitLocationService = createServiceUnitLocationService;
        this.listServiceUnitLocationsService = listServiceUnitLocationsService;
        this.closeServiceUnitService = closeServiceUnitService;
        this.archiveServiceUnitService = archiveServiceUnitService;
    }

    @GetMapping
    @Operation(
            summary = "Lister les unites de service admin",
            description = "Retourne les unites de service d'une entreprise pour l'administration, avec pagination et filtre optionnel par statut.")
    public PageResponse<ServiceUnitResponse> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) ServiceUnitStatus status,
            Pageable pageable) {
        return PageResponse.from(listAdminServiceUnitsService.list(companyId, authenticatedUser, status, pageable));
    }

    @PutMapping("/{serviceUnitId}")
    @Operation(
            summary = "Modifier une unite de service",
            description = "Met a jour les informations administrables d'une unite de service, incluant ses parametres anti-spam.")
    public ServiceUnitResponse update(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateServiceUnitRequest request) {
        return updateServiceUnitService.update(companyId, serviceUnitId, authenticatedUser, request.toCommand());
    }

    @PostMapping("/{serviceUnitId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creer un emplacement",
            description = "Ajoute un emplacement dans une unite de service. L'emplacement peut ensuite etre utilise pour creer des tickets.")
    public ServiceUnitLocationResponse createLocation(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateServiceUnitLocationRequest request) {
        return createServiceUnitLocationService.create(companyId, serviceUnitId, authenticatedUser, request.toCommand());
    }

    @GetMapping("/{serviceUnitId}/locations")
    @Operation(
            summary = "Lister les emplacements",
            description = "Retourne les emplacements d'une unite de service pour l'administration, avec pagination.")
    public PageResponse<ServiceUnitLocationResponse> listLocations(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            Pageable pageable) {
        return PageResponse.from(listServiceUnitLocationsService.list(
                companyId,
                serviceUnitId,
                authenticatedUser,
                pageable));
    }

    @PostMapping("/{serviceUnitId}/close")
    @Operation(
            summary = "Fermer une unite de service",
            description = "Passe une unite de service ouverte en statut ferme afin de la rendre indisponible a la creation de tickets publics.")
    public ServiceUnitResponse close(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return closeServiceUnitService.close(companyId, serviceUnitId, authenticatedUser);
    }

    @PostMapping("/{serviceUnitId}/archive")
    @Operation(
            summary = "Archiver une unite de service",
            description = "Archive une unite de service afin de la retirer des parcours actifs.")
    public ServiceUnitResponse archive(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return archiveServiceUnitService.archive(companyId, serviceUnitId, authenticatedUser);
    }

    @PostMapping("/{serviceUnitId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creer un article",
            description = "Associe un catalogue a une unite de service sous forme d'article disponible pour les tickets.")
    public ItemResponse createItem(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateItemRequest request) {
        return createItemService.create(companyId, serviceUnitId, authenticatedUser, request.toCommand());
    }

    @GetMapping("/{serviceUnitId}/items")
    @Operation(
            summary = "Lister les articles d'une unite de service",
            description = "Retourne les articles d'une unite de service pour l'administration, incluant les articles indisponibles ou archives.")
    public List<ItemResponse> listItems(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return listServiceUnitItemsService.list(companyId, serviceUnitId, authenticatedUser);
    }

    @GetMapping("/{serviceUnitId}/tickets")
    @Operation(
            summary = "Lister les tickets d'une unite de service",
            description = "Retourne les tickets d'une unite de service pour l'administration, avec pagination et filtres optionnels par statut, numero et emplacement.")
    public PageResponse<TicketResponse> listTickets(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String ticketNumber,
            @RequestParam(required = false) UUID locationId,
            Pageable pageable) {
        return PageResponse.from(listServiceUnitTicketsService.list(
                companyId,
                serviceUnitId,
                authenticatedUser,
                status,
                ticketNumber,
                locationId,
                pageable));
    }

    @PatchMapping("/{serviceUnitId}/tickets/{ticketId}/status")
    @Operation(
            summary = "Changer le statut d'un ticket",
            description = "Permet a l'equipe de marquer un ticket comme recu, traite ou annule selon le cycle de vie valide.")
    public TicketResponse changeTicketStatus(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody ChangeTicketStatusRequest request) {
        return changeTicketStatusService.change(
                companyId,
                serviceUnitId,
                ticketId,
                authenticatedUser,
                request.toCommand());
    }

    @PutMapping("/{serviceUnitId}/items/{itemId}")
    @Operation(
            summary = "Modifier un article",
            description = "Met a jour un article rattache a une unite de service, incluant disponibilite, prix et quantites representatives.")
    public ItemResponse updateItem(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateItemRequest request) {
        return updateItemService.update(companyId, serviceUnitId, itemId, authenticatedUser, request.toCommand());
    }
}
