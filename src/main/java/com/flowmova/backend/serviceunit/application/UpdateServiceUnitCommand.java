package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;

public record UpdateServiceUnitCommand(
        String name,
        String description,
        String location,
        TicketCreationGuardMode ticketCreationGuardMode) {
}
