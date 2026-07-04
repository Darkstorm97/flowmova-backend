package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.serviceunit.application.CreateServiceUnitService;
import com.flowmova.backend.serviceunit.application.GetDefaultServiceUnitPublicLinkService;
import com.flowmova.backend.serviceunit.application.GetOpenServiceUnitService;
import com.flowmova.backend.serviceunit.application.ListOpenServiceUnitsService;
import com.flowmova.backend.serviceunit.application.OpenServiceUnitService;
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
    public List<ServiceUnitResponse> listOpen(@PathVariable UUID companyId) {
        return listOpenServiceUnitsService.list(companyId);
    }

    @GetMapping("/{serviceUnitId}")
    public PublicServiceUnitDetailResponse getOpen(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId) {
        return getOpenServiceUnitService.get(companyId, serviceUnitId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceUnitResponse create(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateServiceUnitRequest request) {
        return createServiceUnitService.create(companyId, authenticatedUser, request.toCommand());
    }

    @GetMapping("/{serviceUnitId}/default-public-link")
    public ServiceUnitPublicLinkResponse getDefaultPublicLink(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return getDefaultServiceUnitPublicLinkService.getDefaultPublicLink(companyId, serviceUnitId, authenticatedUser);
    }

    @PostMapping("/{serviceUnitId}/open")
    public ServiceUnitResponse open(
            @PathVariable UUID companyId,
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return openServiceUnitService.open(companyId, serviceUnitId, authenticatedUser);
    }
}
