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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListCompanyTicketsService {

    private final TicketRepository ticketRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final ServiceUnitRepository serviceUnitRepository;

    public ListCompanyTicketsService(
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
            AuthenticatedUser authenticatedUser,
            UUID serviceUnitId,
            TicketStatus status,
            String ticketNumber,
            Pageable pageable) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        requireActiveCompanyMember(companyId, authenticatedUser.userId());
        validateServiceUnit(companyId, serviceUnitId);

        Pageable normalizedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                normalizeSort(pageable.getSort()));
        String normalizedTicketNumber = normalizeTicketNumber(ticketNumber);

        return ticketRepository.findAll(
                        companyTicketsSpec(companyId, serviceUnitId, status, normalizedTicketNumber),
                        normalizedPageable)
                .map(ticket -> TicketResponse.from(ticket, null));
    }

    private void validateServiceUnit(UUID companyId, UUID serviceUnitId) {
        if (serviceUnitId == null) {
            return;
        }

        serviceUnitRepository.findByIdAndCompanyId(serviceUnitId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Service unit is invalid"));
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
            return Sort.by(Sort.Direction.ASC, "createdAt");
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

    private Specification<Ticket> companyTicketsSpec(
            UUID companyId,
            UUID serviceUnitId,
            TicketStatus status,
            String ticketNumber) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.equal(root.get("serviceUnit").get("company").get("id"), companyId);

            if (serviceUnitId != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.equal(root.get("serviceUnit").get("id"), serviceUnitId));
            }

            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }

            if (ticketNumber != null) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(criteriaBuilder.upper(root.get("ticketNumber")), "%" + ticketNumber + "%"));
            }

            return predicate;
        };
    }
}
