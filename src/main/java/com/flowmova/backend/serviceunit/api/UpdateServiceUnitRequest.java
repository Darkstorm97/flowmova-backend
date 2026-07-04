package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.application.UpdateServiceUnitCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateServiceUnitRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2_000) String description,
        @Size(max = 255) String location,
        Boolean oneActiveTicketPerUser) {

    public UpdateServiceUnitCommand toCommand() {
        return new UpdateServiceUnitCommand(name, description, location, oneActiveTicketPerUser);
    }
}
