package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.serviceunit.application.ListAdminServiceUnitsService;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import com.flowmova.backend.shared.api.PageResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/admin/service-units")
public class AdminServiceUnitController {

    private final ListAdminServiceUnitsService listAdminServiceUnitsService;

    public AdminServiceUnitController(ListAdminServiceUnitsService listAdminServiceUnitsService) {
        this.listAdminServiceUnitsService = listAdminServiceUnitsService;
    }

    @GetMapping
    public PageResponse<ServiceUnitResponse> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) ServiceUnitStatus status,
            Pageable pageable) {
        return PageResponse.from(listAdminServiceUnitsService.list(companyId, authenticatedUser, status, pageable));
    }
}
