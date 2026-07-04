package com.flowmova.backend.ticket.api;

import com.flowmova.backend.ticket.application.CancelGuestTicketService;
import com.flowmova.backend.ticket.application.ConfirmGuestTicketTreatmentService;
import com.flowmova.backend.ticket.application.GetGuestTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Guest Tickets", description = "Consultation et actions sur les tickets non authentifies avec numero de ticket et code d'acces.")
public class GuestTicketController {

    private final CancelGuestTicketService cancelGuestTicketService;
    private final ConfirmGuestTicketTreatmentService confirmGuestTicketTreatmentService;
    private final GetGuestTicketService getGuestTicketService;

    public GuestTicketController(
            CancelGuestTicketService cancelGuestTicketService,
            ConfirmGuestTicketTreatmentService confirmGuestTicketTreatmentService,
            GetGuestTicketService getGuestTicketService) {
        this.cancelGuestTicketService = cancelGuestTicketService;
        this.confirmGuestTicketTreatmentService = confirmGuestTicketTreatmentService;
        this.getGuestTicketService = getGuestTicketService;
    }

    @PostMapping("/guest-access")
    @Operation(
            summary = "Consulter un ticket invite",
            description = "Retourne un ticket non authentifie a partir du numero de ticket et du code d'acces.")
    public PublicTicketResponse getGuestTicket(@Valid @RequestBody GetGuestTicketRequest request) {
        return getGuestTicketService.get(request.toCommand());
    }

    @PatchMapping("/guest-access/cancel")
    @Operation(
            summary = "Annuler un ticket invite",
            description = "Annule un ticket non authentifie a partir du numero de ticket et du code d'acces.")
    public PublicTicketResponse cancelGuestTicket(@Valid @RequestBody GetGuestTicketRequest request) {
        return cancelGuestTicketService.cancel(request.toCommand());
    }

    @PatchMapping("/guest-access/confirm-treatment")
    @Operation(
            summary = "Confirmer le traitement d'un ticket invite",
            description = "Permet au visiteur non authentifie de confirmer que son ticket a bien ete traite.")
    public PublicTicketResponse confirmGuestTicketTreatment(@Valid @RequestBody GetGuestTicketRequest request) {
        return confirmGuestTicketTreatmentService.confirm(request.toCommand());
    }
}
