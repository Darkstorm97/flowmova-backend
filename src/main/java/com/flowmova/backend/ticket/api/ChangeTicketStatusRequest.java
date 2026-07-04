package com.flowmova.backend.ticket.api;

import com.flowmova.backend.ticket.application.ChangeTicketStatusCommand;
import com.flowmova.backend.ticket.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTicketStatusRequest(@NotNull TicketStatus status) {

    public ChangeTicketStatusCommand toCommand() {
        return new ChangeTicketStatusCommand(status);
    }
}
