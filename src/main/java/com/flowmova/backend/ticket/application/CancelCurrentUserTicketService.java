package com.flowmova.backend.ticket.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.ticket.api.TicketResponse;
import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CancelCurrentUserTicketService {

    private final TicketRepository ticketRepository;

    public CancelCurrentUserTicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketResponse cancel(UUID ticketId, AuthenticatedUser authenticatedUser) {
        Ticket ticket = ticketRepository.findByIdAndUserId(ticketId, authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        try {
            ticket.cancel();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }

        return TicketResponse.from(ticketRepository.saveAndFlush(ticket), null);
    }
}
