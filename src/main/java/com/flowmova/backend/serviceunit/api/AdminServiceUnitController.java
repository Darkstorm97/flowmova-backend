package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.item.api.CreateItemRequest;
import com.flowmova.backend.item.api.ItemResponse;
import com.flowmova.backend.item.api.UpdateItemRequest;
import com.flowmova.backend.item.application.CreateItemService;
import com.flowmova.backend.item.application.UpdateItemService;
import com.flowmova.backend.serviceunit.application.ListAdminServiceUnitsService;
import com.flowmova.backend.serviceunit.application.UpdateServiceUnitService;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    public AdminServiceUnitController(
            ListAdminServiceUnitsService listAdminServiceUnitsService,
            UpdateServiceUnitService updateServiceUnitService,
            CreateItemService createItemService,
            UpdateItemService updateItemService) {
        this.listAdminServiceUnitsService = listAdminServiceUnitsService;
        this.updateServiceUnitService = updateServiceUnitService;
        this.createItemService = createItemService;
        this.updateItemService = updateItemService;
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

    @PostMapping("/{serviceUnitId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse createItem(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateItemRequest request) {
        return createItemService.create(companyId, serviceUnitId, authenticatedUser, request.toCommand());
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
