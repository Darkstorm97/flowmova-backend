package com.flowmova.backend.ticket.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.ticket.api.TicketResponse;
import com.flowmova.backend.ticket.domain.TicketStatus;
import com.flowmova.backend.ticket.infrastructure.TicketRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListCurrentUserTicketsService {

    private final TicketRepository ticketRepository;

    public ListCurrentUserTicketsService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> list(
            AuthenticatedUser authenticatedUser,
            TicketStatus status,
            String ticketNumber,
            Pageable pageable) {
        Pageable normalizedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                normalizeSort(pageable.getSort()));

        String normalizedTicketNumber = normalizeTicketNumber(ticketNumber);

        if (status != null && normalizedTicketNumber != null) {
            return ticketRepository.findByUserIdAndStatusAndTicketNumberContainingIgnoreCase(
                            authenticatedUser.userId(),
                            status,
                            normalizedTicketNumber,
                            normalizedPageable)
                    .map(ticket -> TicketResponse.from(ticket, null));
        }

        if (status != null) {
            return ticketRepository.findByUserIdAndStatus(
                            authenticatedUser.userId(),
                            status,
                            normalizedPageable)
                    .map(ticket -> TicketResponse.from(ticket, null));
        }

        if (normalizedTicketNumber != null) {
            return ticketRepository.findByUserIdAndTicketNumberContainingIgnoreCase(
                            authenticatedUser.userId(),
                            normalizedTicketNumber,
                            normalizedPageable)
                    .map(ticket -> TicketResponse.from(ticket, null));
        }

        return ticketRepository.findByUserId(authenticatedUser.userId(), normalizedPageable)
                .map(ticket -> TicketResponse.from(ticket, null));
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
