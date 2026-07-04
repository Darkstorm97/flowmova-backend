package com.flowmova.backend.ticket.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.domain.TicketStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TicketResponse(
        UUID id,
        String ticketNumber,
        String accessCode,
        UUID userId,
        String guestName,
        String customerPhone,
        UUID serviceUnitId,
        UUID locationId,
        TicketStatus status,
        String notes,
        String currency,
        BigDecimal totalAmount,
        List<TicketLineResponse> lines,
        Instant createdAt,
        Instant updatedAt) {

    public static TicketResponse from(Ticket ticket, String accessCode) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                accessCode,
                ticket.getUser() == null ? null : ticket.getUser().getId(),
                ticket.getGuestName(),
                ticket.getCustomerPhone(),
                ticket.getServiceUnit().getId(),
                ticket.getServiceUnitLocation().getId(),
                ticket.getStatus(),
                ticket.getNotes(),
                ticket.getCurrency(),
                ticket.getTotalAmount(),
                ticket.getLines().stream().map(TicketLineResponse::from).toList(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt());
    }
}
