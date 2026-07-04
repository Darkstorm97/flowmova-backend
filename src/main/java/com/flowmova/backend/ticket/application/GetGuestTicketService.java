package com.flowmova.backend.ticket.application;

import com.flowmova.backend.ticket.api.PublicTicketResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetGuestTicketService {

    private final GuestTicketAccessValidator guestTicketAccessValidator;

    public GetGuestTicketService(GuestTicketAccessValidator guestTicketAccessValidator) {
        this.guestTicketAccessValidator = guestTicketAccessValidator;
    }

    @Transactional(readOnly = true)
    public PublicTicketResponse get(GetGuestTicketCommand command) {
        return PublicTicketResponse.from(guestTicketAccessValidator.requireGuestTicket(
                command.ticketNumber(),
                command.accessCode()));
    }
}
