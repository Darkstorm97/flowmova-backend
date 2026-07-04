package com.flowmova.backend.ticket.infrastructure;

import com.flowmova.backend.ticket.domain.TicketLine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketLineRepository extends JpaRepository<TicketLine, UUID> {

    List<TicketLine> findByTicketId(UUID ticketId);
}
