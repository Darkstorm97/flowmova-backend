package com.flowmova.backend.ticket.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.shared.api.PageResponse;
import com.flowmova.backend.ticket.application.ListCompanyTicketsService;
import com.flowmova.backend.ticket.domain.TicketStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies/{companyId}/admin/tickets")
@Tag(name = "Admin Tickets", description = "Suivi global des tickets d'une entreprise.")
public class AdminTicketController {

    private final ListCompanyTicketsService listCompanyTicketsService;

    public AdminTicketController(ListCompanyTicketsService listCompanyTicketsService) {
        this.listCompanyTicketsService = listCompanyTicketsService;
    }

    @GetMapping
    @Operation(
            summary = "Lister les tickets d'une entreprise",
            description = "Retourne les tickets de tous les services d'une entreprise pour l'administration, avec pagination et filtres optionnels par service, statut et numero.")
    public PageResponse<TicketResponse> list(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) UUID serviceUnitId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String ticketNumber,
            Pageable pageable) {
        return PageResponse.from(listCompanyTicketsService.list(
                companyId,
                authenticatedUser,
                serviceUnitId,
                status,
                ticketNumber,
                pageable));
    }
}
