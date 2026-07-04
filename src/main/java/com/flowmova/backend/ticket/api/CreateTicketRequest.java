package com.flowmova.backend.ticket.api;

import com.flowmova.backend.ticket.application.CreateTicketCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record CreateTicketRequest(
        UUID locationId,
        @Size(max = 150) String guestName,
        @Size(max = 40) String customerPhone,
        String notes,
        @Valid List<CreateTicketLineRequest> lines) {

    CreateTicketCommand toCommand() {
        return new CreateTicketCommand(
                locationId,
                guestName,
                customerPhone,
                notes,
                lines == null ? List.of() : lines.stream()
                        .map(CreateTicketLineRequest::toCommand)
                        .toList());
    }
}
