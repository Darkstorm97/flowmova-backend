package com.flowmova.backend.ticket.application;

import com.flowmova.backend.ticket.api.PublicTicketResponse;
import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GetGuestTicketService {

    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public GetGuestTicketService(TicketRepository ticketRepository, PasswordEncoder passwordEncoder) {
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PublicTicketResponse get(GetGuestTicketCommand command) {
        Ticket ticket = ticketRepository.findByTicketNumber(command.ticketNumber().trim())
                .orElseThrow(this::invalidAccess);

        String accessCodeHash = ticket.getGuestAccessCodeHash();
        if (accessCodeHash == null || !passwordEncoder.matches(command.accessCode().trim(), accessCodeHash)) {
            throw invalidAccess();
        }

        return PublicTicketResponse.from(ticket);
    }

    private ResponseStatusException invalidAccess() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Ticket access is invalid");
    }
}
