package com.flowmova.backend.ticket.infrastructure;

import com.flowmova.backend.ticket.domain.Ticket;
import com.flowmova.backend.ticket.domain.TicketStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByServiceUnitIdOrderByCreatedAtDesc(UUID serviceUnitId);

    List<Ticket> findByServiceUnitIdAndStatusOrderByCreatedAtDesc(UUID serviceUnitId, TicketStatus status);

    List<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Ticket> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, TicketStatus status);
}
