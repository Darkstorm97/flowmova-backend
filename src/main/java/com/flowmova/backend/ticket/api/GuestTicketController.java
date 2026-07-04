package com.flowmova.backend.ticket.api;

import com.flowmova.backend.ticket.application.GetGuestTicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class GuestTicketController {

    private final GetGuestTicketService getGuestTicketService;

    public GuestTicketController(GetGuestTicketService getGuestTicketService) {
        this.getGuestTicketService = getGuestTicketService;
    }

    @PostMapping("/guest-access")
    public PublicTicketResponse getGuestTicket(@Valid @RequestBody GetGuestTicketRequest request) {
        return getGuestTicketService.get(request.toCommand());
    }
}
