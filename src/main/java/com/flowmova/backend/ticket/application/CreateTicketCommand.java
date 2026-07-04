package com.flowmova.backend.ticket.application;

import java.util.List;
import java.util.UUID;

public record CreateTicketCommand(
        UUID locationId,
        String guestName,
        String customerPhone,
        String notes,
        List<CreateTicketLineCommand> lines) {
}
