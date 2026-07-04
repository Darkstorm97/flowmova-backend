package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.serviceunit.application.CreateServiceUnitService;
import com.flowmova.backend.serviceunit.application.GetDefaultServiceUnitPublicLinkService;
import jakarta.validation.Valid;
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

    public ServiceUnitController(
            CreateServiceUnitService createServiceUnitService,
            GetDefaultServiceUnitPublicLinkService getDefaultServiceUnitPublicLinkService) {
        this.createServiceUnitService = createServiceUnitService;
        this.getDefaultServiceUnitPublicLinkService = getDefaultServiceUnitPublicLinkService;
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
}
