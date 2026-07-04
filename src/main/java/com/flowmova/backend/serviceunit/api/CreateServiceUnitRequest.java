package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.application.CreateServiceUnitCommand;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateServiceUnitRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        String description,

        @Size(max = 255)
        String location,

        @NotNull
        ServiceUnitType type,

        Boolean oneActiveTicketPerUser) {

    public CreateServiceUnitCommand toCommand() {
        return new CreateServiceUnitCommand(
                name,
                description,
                location,
                type,
                Boolean.TRUE.equals(oneActiveTicketPerUser));
    }
}
