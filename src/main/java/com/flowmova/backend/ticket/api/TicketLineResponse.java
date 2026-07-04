package com.flowmova.backend.ticket.api;

import com.flowmova.backend.ticket.domain.TicketLine;
import java.math.BigDecimal;
import java.util.UUID;

public record TicketLineResponse(
        UUID id,
        UUID itemId,
        Integer quantity,
        BigDecimal unitPriceAmount,
        BigDecimal lineTotalAmount,
        String notes) {

    static TicketLineResponse from(TicketLine line) {
        return new TicketLineResponse(
                line.getId(),
                line.getItem().getId(),
                line.getQuantity(),
                line.getUnitPriceAmount(),
                line.getLineTotalAmount(),
                line.getNotes());
    }
}
