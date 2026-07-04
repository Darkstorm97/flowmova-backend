package com.flowmova.backend.ticket.api;

import com.flowmova.backend.ticket.application.GetGuestTicketCommand;
import jakarta.validation.constraints.NotBlank;

public record GetGuestTicketRequest(
        @NotBlank String ticketNumber,
        @NotBlank String accessCode) {

    GetGuestTicketCommand toCommand() {
        return new GetGuestTicketCommand(ticketNumber, accessCode);
    }
}
