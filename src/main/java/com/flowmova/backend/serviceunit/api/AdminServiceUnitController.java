package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.item.api.CreateItemRequest;
import com.flowmova.backend.item.api.ItemResponse;
import com.flowmova.backend.item.api.UpdateItemRequest;
import com.flowmova.backend.item.application.CreateItemService;
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
import jakarta.validation.Valid;
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
public class AdminServiceUnitController {

    private final ListAdminServiceUnitsService listAdminServiceUnitsService;
    private final UpdateServiceUnitService updateServiceUnitService;
    private final CreateItemService createItemService;
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
        this.updateItemService = updateItemService;
        this.listServiceUnitTicketsService = listServiceUnitTicketsService;
        this.changeTicketStatusService = changeTicketStatusService;
        this.createServiceUnitLocationService = createServiceUnitLocationService;
        this.listServiceUnitLocationsService = listServiceUnitLocationsService;
        this.closeServiceUnitService = closeServiceUnitService;
        this.archiveServiceUnitService = archiveServiceUnitService;
    }

    @GetMapping
    public PageResponse<ServiceUnitResponse> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) ServiceUnitStatus status,
            Pageable pageable) {
        return PageResponse.from(listAdminServiceUnitsService.list(companyId, authenticatedUser, status, pageable));
    }

    @PutMapping("/{serviceUnitId}")
    public ServiceUnitResponse update(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateServiceUnitRequest request) {
        return updateServiceUnitService.update(companyId, serviceUnitId, authenticatedUser, request.toCommand());
    }

    @PostMapping("/{serviceUnitId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceUnitLocationResponse createLocation(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateServiceUnitLocationRequest request) {
        return createServiceUnitLocationService.create(companyId, serviceUnitId, authenticatedUser, request.toCommand());
    }

    @GetMapping("/{serviceUnitId}/locations")
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
    public ServiceUnitResponse close(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return closeServiceUnitService.close(companyId, serviceUnitId, authenticatedUser);
    }

    @PostMapping("/{serviceUnitId}/archive")
    public ServiceUnitResponse archive(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return archiveServiceUnitService.archive(companyId, serviceUnitId, authenticatedUser);
    }

    @PostMapping("/{serviceUnitId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse createItem(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateItemRequest request) {
        return createItemService.create(companyId, serviceUnitId, authenticatedUser, request.toCommand());
    }

    @GetMapping("/{serviceUnitId}/tickets")
    public PageResponse<TicketResponse> listTickets(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String ticketNumber,
            Pageable pageable) {
        return PageResponse.from(listServiceUnitTicketsService.list(
                companyId,
                serviceUnitId,
                authenticatedUser,
                status,
                ticketNumber,
                pageable));
    }

    @PatchMapping("/{serviceUnitId}/tickets/{ticketId}/status")
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
    public ItemResponse updateItem(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @PathVariable UUID itemId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateItemRequest request) {
        return updateItemService.update(companyId, serviceUnitId, itemId, authenticatedUser, request.toCommand());
    }
}
