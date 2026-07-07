package com.flowmova.backend.ticket.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.ticket.application.CreateTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/locations/{publicAccessSlug}/tickets")
@Tag(name = "Public Location Tickets", description = "Creation de tickets depuis un lien public d'emplacement.")
public class PublicLocationTicketController {

    private final CreateTicketService createTicketService;

    public PublicLocationTicketController(CreateTicketService createTicketService) {
        this.createTicketService = createTicketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creer un ticket depuis un emplacement public",
            description = "Cree un ticket depuis un slug public d'emplacement. L'emplacement du ticket est force par le slug QR.")
    public TicketResponse create(
            @PathVariable String publicAccessSlug,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateTicketRequest request) {
        return createTicketService.createFromPublicLocation(
                publicAccessSlug,
                authenticatedUser,
                request.toCommand());
    }
}
