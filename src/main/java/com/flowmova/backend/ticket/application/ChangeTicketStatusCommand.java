package com.flowmova.backend.ticket.application;

import com.flowmova.backend.ticket.domain.TicketStatus;

public record ChangeTicketStatusCommand(TicketStatus status) {
}
