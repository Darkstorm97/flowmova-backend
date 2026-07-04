package com.flowmova.backend.ticket.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.shared.api.PageResponse;
import com.flowmova.backend.ticket.application.CancelCurrentUserTicketService;
import com.flowmova.backend.ticket.application.ConfirmCurrentUserTicketTreatmentService;
import com.flowmova.backend.ticket.application.ListCurrentUserTicketsService;
import com.flowmova.backend.ticket.domain.TicketStatus;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/tickets")
public class CurrentUserTicketController {

    private final ListCurrentUserTicketsService listCurrentUserTicketsService;
    private final CancelCurrentUserTicketService cancelCurrentUserTicketService;
    private final ConfirmCurrentUserTicketTreatmentService confirmCurrentUserTicketTreatmentService;

    public CurrentUserTicketController(
            ListCurrentUserTicketsService listCurrentUserTicketsService,
            CancelCurrentUserTicketService cancelCurrentUserTicketService,
            ConfirmCurrentUserTicketTreatmentService confirmCurrentUserTicketTreatmentService) {
        this.listCurrentUserTicketsService = listCurrentUserTicketsService;
        this.cancelCurrentUserTicketService = cancelCurrentUserTicketService;
        this.confirmCurrentUserTicketTreatmentService = confirmCurrentUserTicketTreatmentService;
    }

    @GetMapping
    public PageResponse<TicketResponse> list(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) String ticketNumber,
            Pageable pageable) {
        return PageResponse.from(listCurrentUserTicketsService.list(
                authenticatedUser,
                status,
                ticketNumber,
                pageable));
    }

    @PatchMapping("/{ticketId}/cancel")
    public TicketResponse cancel(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return cancelCurrentUserTicketService.cancel(ticketId, authenticatedUser);
    }

    @PatchMapping("/{ticketId}/confirm-treatment")
    public TicketResponse confirmTreatment(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return confirmCurrentUserTicketTreatmentService.confirm(ticketId, authenticatedUser);
    }
}
