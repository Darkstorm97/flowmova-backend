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
import com.flowmova.backend.ticket.domain.TicketStatus;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListServiceUnitTicketsService {

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final ServiceUnitRepository serviceUnitRepository;

    public ListServiceUnitTicketsService(
            TicketRepository ticketRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            ServiceUnitRepository serviceUnitRepository) {
        this.ticketRepository = ticketRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.serviceUnitRepository = serviceUnitRepository;
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(
            UUID companyId,
            UUID serviceUnitId,
            AuthenticatedUser authenticatedUser,
            TicketStatus status,
            String ticketNumber,
            Pageable pageable) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        requireActiveCompanyMember(companyId, authenticatedUser.userId());

        serviceUnitRepository.findByIdAndCompanyId(serviceUnitId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service unit not found"));

        Pageable normalizedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                normalizeSort(pageable.getSort()));
        String normalizedTicketNumber = normalizeTicketNumber(ticketNumber);

        if (status != null && normalizedTicketNumber != null) {
            return ticketRepository.findByServiceUnitIdAndStatusAndTicketNumberContainingIgnoreCase(
                            serviceUnitId,
                            status,
                            normalizedTicketNumber,
                            normalizedPageable)
                    .map(ticket -> TicketResponse.from(ticket, null));
        }

        if (status != null) {
            return ticketRepository.findByServiceUnitIdAndStatus(serviceUnitId, status, normalizedPageable)
                    .map(ticket -> TicketResponse.from(ticket, null));
        }

        if (normalizedTicketNumber != null) {
            return ticketRepository.findByServiceUnitIdAndTicketNumberContainingIgnoreCase(
                            serviceUnitId,
                            normalizedTicketNumber,
                            normalizedPageable)
                    .map(ticket -> TicketResponse.from(ticket, null));
        }

        return ticketRepository.findByServiceUnitId(serviceUnitId, normalizedPageable)
                .map(ticket -> TicketResponse.from(ticket, null));
    }

    private void requireActiveCompanyMember(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company member role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE
                || (companyUser.getRole() != CompanyRole.ADMIN && companyUser.getRole() != CompanyRole.EMPLOYEE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company member role is required");
        }
    }

    private Sort normalizeSort(Sort requestedSort) {
        if (requestedSort.isUnsorted()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return Sort.by(requestedSort.stream()
                .map(order -> new Sort.Order(order.getDirection(), sortProperty(order.getProperty())))
                .toList());
    }

    private String sortProperty(String requestedProperty) {
        return switch (requestedProperty) {
            case "ticketNumber" -> "ticketNumber";
            case "status" -> "status";
            case "totalAmount" -> "totalAmount";
            case "createdAt" -> "createdAt";
            default -> "createdAt";
        };
    }

    private String normalizeTicketNumber(String ticketNumber) {
        if (ticketNumber == null) {
            return null;
        }
        String normalized = ticketNumber.trim().replace(" ", "").toUpperCase();
        return normalized.isBlank() ? null : normalized;
    }
}
