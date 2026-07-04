package com.flowmova.backend.ticket.application;

import java.util.UUID;

public record CreateTicketLineCommand(
        UUID itemId,
        Integer quantity,
        String notes) {
}
