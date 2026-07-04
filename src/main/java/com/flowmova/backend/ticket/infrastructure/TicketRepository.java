package com.flowmova.backend.ticket.infrastructure;

import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.domain.TicketStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    Optional<Ticket> findByIdAndServiceUnitId(UUID id, UUID serviceUnitId);

    Optional<Ticket> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndServiceUnitIdAndStatusIn(
            UUID userId,
            UUID serviceUnitId,
            Collection<TicketStatus> statuses);

    List<Ticket> findByServiceUnitIdOrderByCreatedAtDesc(UUID serviceUnitId);

    List<Ticket> findByServiceUnitIdAndStatusOrderByCreatedAtDesc(UUID serviceUnitId, TicketStatus status);

    List<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Ticket> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TicketStatus status);

    Page<Ticket> findByUserId(UUID userId, Pageable pageable);

    Page<Ticket> findByUserIdAndStatus(UUID userId, TicketStatus status, Pageable pageable);

    Page<Ticket> findByUserIdAndTicketNumberContainingIgnoreCase(
            UUID userId,
            String ticketNumber,
            Pageable pageable);

    Page<Ticket> findByUserIdAndStatusAndTicketNumberContainingIgnoreCase(
            UUID userId,
            TicketStatus status,
            String ticketNumber,
            Pageable pageable);

    Page<Ticket> findByServiceUnitId(UUID serviceUnitId, Pageable pageable);

    Page<Ticket> findByServiceUnitIdAndStatus(UUID serviceUnitId, TicketStatus status, Pageable pageable);

    Page<Ticket> findByServiceUnitIdAndTicketNumberContainingIgnoreCase(
            UUID serviceUnitId,
            String ticketNumber,
            Pageable pageable);

    Page<Ticket> findByServiceUnitIdAndStatusAndTicketNumberContainingIgnoreCase(
            UUID serviceUnitId,
            TicketStatus status,
            String ticketNumber,
            Pageable pageable);
}
