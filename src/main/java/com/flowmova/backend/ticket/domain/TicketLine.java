package com.flowmova.backend.ticket.domain;

import com.flowmova.backend.item.domain.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ticket_lines")
public class TicketLine {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price_amount", precision = 12, scale = 2)
    private BigDecimal unitPriceAmount;

    @Column(name = "line_total_amount", precision = 12, scale = 2)
    private BigDecimal lineTotalAmount;

    @Column(name = "notes")
    private String notes;

    protected TicketLine() {
    }

    public TicketLine(Item item, Integer quantity, BigDecimal unitPriceAmount, String notes) {
        this.id = UUID.randomUUID();
        this.item = item;
        this.quantity = quantity == null ? 1 : quantity;
        if (this.quantity < 1) {
            throw new IllegalArgumentException("Ticket line quantity must be greater than or equal to 1");
        }
        this.unitPriceAmount = unitPriceAmount;
        this.lineTotalAmount = unitPriceAmount == null
                ? null
                : unitPriceAmount.multiply(BigDecimal.valueOf(this.quantity));
        this.notes = notes;
    }

    void attachTo(Ticket ticket) {
        validateItemBelongsToTicketServiceUnit(ticket, item);
        this.ticket = ticket;
    }

    private static void validateItemBelongsToTicketServiceUnit(Ticket ticket, Item item) {
        if (!Objects.equals(ticket.getServiceUnit().getId(), item.getServiceUnit().getId())) {
            throw new IllegalArgumentException("Item must belong to the same service unit as ticket");
        }
    }

    public UUID getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public Item getItem() {
        return item;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPriceAmount() {
        return unitPriceAmount;
    }

    public BigDecimal getLineTotalAmount() {
        return lineTotalAmount;
    }

    public String getNotes() {
        return notes;
    }
}
