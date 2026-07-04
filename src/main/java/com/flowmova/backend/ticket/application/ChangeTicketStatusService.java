package com.flowmova.backend.ticket.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.ticket.api.TicketResponse;
import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.domain.TicketStatus;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChangeTicketStatusService {

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final ServiceUnitRepository serviceUnitRepository;

    public ChangeTicketStatusService(
            TicketRepository ticketRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            ServiceUnitRepository serviceUnitRepository) {
        this.ticketRepository = ticketRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.serviceUnitRepository = serviceUnitRepository;
    }

    @Transactional
    public TicketResponse change(
            UUID companyId,
            UUID serviceUnitId,
            UUID ticketId,
            AuthenticatedUser authenticatedUser,
            ChangeTicketStatusCommand command) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
        requireActiveCompanyMember(companyId, authenticatedUser.userId());
        serviceUnitRepository.findByIdAndCompanyId(serviceUnitId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service unit not found"));

        Ticket ticket = ticketRepository.findByIdAndServiceUnitId(ticketId, serviceUnitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        applyTransition(ticket, command.status());
        return TicketResponse.from(ticketRepository.saveAndFlush(ticket), null);
    }

    private void requireActiveCompanyMember(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company member role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE
                || (companyUser.getRole() != CompanyRole.ADMIN && companyUser.getRole() != CompanyRole.EMPLOYEE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company member role is required");
        }
    }

    private void applyTransition(Ticket ticket, TicketStatus targetStatus) {
        try {
            switch (targetStatus) {
                case RECEIVED -> ticket.markReceived();
                case TREATED -> ticket.markTreated();
                case CANCELLED -> ticket.cancel();
                case CLOSED -> ticket.close();
                default -> throw new IllegalStateException("Ticket transition is invalid");
            }
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }
}
