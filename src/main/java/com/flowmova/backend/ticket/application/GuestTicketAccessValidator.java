package com.flowmova.backend.ticket.application;

import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class GuestTicketAccessValidator {

    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    GuestTicketAccessValidator(TicketRepository ticketRepository, PasswordEncoder passwordEncoder) {
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    Ticket requireGuestTicket(String ticketNumber, String accessCode) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber.trim())
                .orElseThrow(this::invalidAccess);

        String accessCodeHash = ticket.getGuestAccessCodeHash();
        if (accessCodeHash == null || !passwordEncoder.matches(accessCode.trim(), accessCodeHash)) {
            throw invalidAccess();
        }

        return ticket;
    }

    private ResponseStatusException invalidAccess() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Ticket access is invalid");
    }
}
