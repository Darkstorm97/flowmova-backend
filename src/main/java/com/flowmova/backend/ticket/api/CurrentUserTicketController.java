package com.flowmova.backend.ticket.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.shared.api.PageResponse;
import com.flowmova.backend.ticket.application.ListCurrentUserTicketsService;
import com.flowmova.backend.ticket.domain.TicketStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/tickets")
public class CurrentUserTicketController {

    private final ListCurrentUserTicketsService listCurrentUserTicketsService;

    public CurrentUserTicketController(ListCurrentUserTicketsService listCurrentUserTicketsService) {
        this.listCurrentUserTicketsService = listCurrentUserTicketsService;
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
}
