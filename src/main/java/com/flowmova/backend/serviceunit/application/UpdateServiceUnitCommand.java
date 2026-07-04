package com.flowmova.backend.serviceunit.application;

public record UpdateServiceUnitCommand(
        String name,
        String description,
        String location,
        Boolean oneActiveTicketPerUser) {
}
