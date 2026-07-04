package com.flowmova.backend.ticket.application;

public record GetGuestTicketCommand(
        String ticketNumber,
        String accessCode) {
}
