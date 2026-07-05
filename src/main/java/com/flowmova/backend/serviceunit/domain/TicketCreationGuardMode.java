package com.flowmova.backend.serviceunit.domain;

public enum TicketCreationGuardMode {
    NONE,
    AUTHENTICATED_ONLY_ONE_OPEN_TICKET,
    AUTHENTICATED_OR_GUEST_RECENT_ONE_OPEN_TICKET
}
