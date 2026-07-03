package com.flowmova.backend.company.api;

import com.flowmova.backend.company.application.CreateCompanyCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCompanyRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2_000) String description,
        @Size(max = 3) String currency) {

    public CreateCompanyCommand toCommand() {
        return new CreateCompanyCommand(name, description, currency);
    }
}
