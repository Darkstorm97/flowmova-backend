package com.flowmova.backend.ticket.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.ticket.application.CreateTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-units/{serviceUnitId}/tickets")
@Tag(name = "Tickets", description = "Creation de tickets par utilisateur authentifie ou visiteur non authentifie.")
public class TicketController {

    private final CreateTicketService createTicketService;

    public TicketController(CreateTicketService createTicketService) {
        this.createTicketService = createTicketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creer un ticket",
            description = "Cree un ticket dans une unite de service. JWT optionnel: avec JWT le ticket est rattache a l'utilisateur, sans JWT guestName est requis.")
    public TicketResponse create(
            @PathVariable UUID serviceUnitId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateTicketRequest request) {
        return createTicketService.create(serviceUnitId, authenticatedUser, request.toCommand());
    }
}
