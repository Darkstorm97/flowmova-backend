package com.flowmova.backend.ticket.api;

import com.flowmova.backend.ticket.application.CancelGuestTicketService;
import com.flowmova.backend.ticket.application.ConfirmGuestTicketTreatmentService;
import com.flowmova.backend.ticket.application.GetGuestTicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
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
    public PublicTicketResponse getGuestTicket(@Valid @RequestBody GetGuestTicketRequest request) {
        return getGuestTicketService.get(request.toCommand());
    }

    @PatchMapping("/guest-access/cancel")
    public PublicTicketResponse cancelGuestTicket(@Valid @RequestBody GetGuestTicketRequest request) {
        return cancelGuestTicketService.cancel(request.toCommand());
    }

    @PatchMapping("/guest-access/confirm-treatment")
    public PublicTicketResponse confirmGuestTicketTreatment(@Valid @RequestBody GetGuestTicketRequest request) {
        return confirmGuestTicketTreatmentService.confirm(request.toCommand());
    }
}
