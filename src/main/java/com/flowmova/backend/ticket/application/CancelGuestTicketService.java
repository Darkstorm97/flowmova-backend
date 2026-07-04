package com.flowmova.backend.ticket.application;

import com.flowmova.backend.ticket.api.PublicTicketResponse;
import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CancelGuestTicketService {

    private final GuestTicketAccessValidator guestTicketAccessValidator;
    private final TicketRepository ticketRepository;

    public CancelGuestTicketService(
            GuestTicketAccessValidator guestTicketAccessValidator,
            TicketRepository ticketRepository) {
        this.guestTicketAccessValidator = guestTicketAccessValidator;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public PublicTicketResponse cancel(GetGuestTicketCommand command) {
        Ticket ticket = guestTicketAccessValidator.requireGuestTicket(command.ticketNumber(), command.accessCode());

        try {
            ticket.cancel();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }

        return PublicTicketResponse.from(ticketRepository.saveAndFlush(ticket));
    }
}
