package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunitlocation.application.CreateServiceUnitLocationCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateServiceUnitLocationRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        String description) {

    CreateServiceUnitLocationCommand toCommand() {
        return new CreateServiceUnitLocationCommand(name, description);
    }
}
