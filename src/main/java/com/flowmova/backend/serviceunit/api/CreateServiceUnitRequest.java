package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.application.CreateServiceUnitCommand;
import com.flowmova.backend.serviceunit.domain.ServiceUnitCreationEntryMode;
import com.flowmova.backend.serviceunit.domain.ServiceUnitType;
import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
import io.swagger.v3.oas.annotations.media.Schema;
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

        @Schema(description = "Mode de controle anti-spam de creation de tickets. Si absent, NONE est utilise.")
        TicketCreationGuardMode ticketCreationGuardMode,

        @Schema(description = "Mode d'entree autorise pour creer un ticket. Si absent, PUBLIC_AND_QR est utilise.")
        ServiceUnitCreationEntryMode creationEntryMode,

        @Schema(description = "Autorise la creation d'un ticket sans article. Si absent, true est utilise.")
        Boolean allowTicketWithoutItems) {

    public CreateServiceUnitCommand toCommand() {
        return new CreateServiceUnitCommand(
                name,
                description,
                location,
                type,
                ticketCreationGuardMode == null ? TicketCreationGuardMode.NONE : ticketCreationGuardMode,
                creationEntryMode == null ? ServiceUnitCreationEntryMode.PUBLIC_AND_QR : creationEntryMode,
                allowTicketWithoutItems == null ? true : allowTicketWithoutItems);
    }
}
