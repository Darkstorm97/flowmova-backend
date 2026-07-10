package com.flowmova.backend.serviceunit.api;

import com.flowmova.backend.serviceunit.application.UpdateServiceUnitCommand;
import com.flowmova.backend.serviceunit.domain.ServiceUnitCreationEntryMode;
import com.flowmova.backend.serviceunit.domain.TicketCreationGuardMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateServiceUnitRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2_000) String description,
        @Size(max = 255) String location,
        @Schema(description = "Mode de controle anti-spam de creation de tickets. Si absent, le mode actuel est conserve.")
        TicketCreationGuardMode ticketCreationGuardMode,
        @Schema(description = "Mode d'entree autorise pour creer un ticket. Si absent, le mode actuel est conserve.")
        ServiceUnitCreationEntryMode creationEntryMode,
        @Schema(description = "Autorise la creation d'un ticket sans article. Si absent, le mode actuel est conserve.")
        Boolean allowTicketWithoutItems) {

    public UpdateServiceUnitCommand toCommand() {
        return new UpdateServiceUnitCommand(
                name,
                description,
                location,
                ticketCreationGuardMode,
                creationEntryMode,
                allowTicketWithoutItems);
    }
}
