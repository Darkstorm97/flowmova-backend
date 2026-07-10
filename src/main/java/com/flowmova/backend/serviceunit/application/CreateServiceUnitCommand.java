package com.flowmova.backend.serviceunit.application;

import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
import com.flowmova.backend.serviceunit.domain.ServiceUnitCreationEntryMode;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;

public record CreateServiceUnitCommand(
        String name,
        String description,
        String location,
        ServiceUnitType type,
        TicketCreationGuardMode ticketCreationGuardMode,
        ServiceUnitCreationEntryMode creationEntryMode,
        Boolean allowTicketWithoutItems) {
}
