package com.flowmova.backend.ticket.api;

import com.flowmova.backend.ticket.application.CreateTicketLineCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateTicketLineRequest(
        @NotNull UUID itemId,
        @Min(1) Integer quantity,
        String notes) {

    CreateTicketLineCommand toCommand() {
        return new CreateTicketLineCommand(itemId, quantity, notes);
    }
}
