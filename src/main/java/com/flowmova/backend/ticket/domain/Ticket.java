package com.flowmova.backend.ticket.domain;

import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.user.domain.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "ticket_number", nullable = false, length = 40)
    private String ticketNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_name", length = 150)
    private String guestName;

    @Column(name = "customer_phone", length = 40)
    private String customerPhone;

    @Column(name = "guest_access_code_hash")
    private String guestAccessCodeHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_unit_id", nullable = false)
    private ServiceUnit serviceUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_unit_location_id", nullable = false)
    private ServiceUnitLocation serviceUnitLocation;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "ticket_status")
    private TicketStatus status = TicketStatus.CREATED;

    @Column(name = "notes")
    private String notes;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TicketLine> lines = new ArrayList<>();

    protected Ticket() {
    }

    public Ticket(
            String ticketNumber,
            User user,
            String guestName,
            String guestAccessCodeHash,
            ServiceUnit serviceUnit,
            ServiceUnitLocation serviceUnitLocation,
            String customerPhone,
            String notes,
            String currency) {
        validateLocationBelongsToServiceUnit(serviceUnit, serviceUnitLocation);
        validateCreator(user, guestName, guestAccessCodeHash);
        this.id = UUID.randomUUID();
        this.ticketNumber = ticketNumber;
        this.user = user;
        this.guestName = guestName;
        this.guestAccessCodeHash = guestAccessCodeHash;
        this.serviceUnit = serviceUnit;
        this.serviceUnitLocation = serviceUnitLocation;
        this.customerPhone = customerPhone;
        this.notes = notes;
        this.currency = currency;
    }

    public void addLine(TicketLine line) {
        line.attachTo(this);
        lines.add(line);
        recalculateTotalAmount();
    }

    public void markReceived() {
        transitionTo(TicketStatus.RECEIVED, TicketStatus.CREATED);
    }

    public void markTreated() {
        if (status != TicketStatus.CREATED && status != TicketStatus.RECEIVED) {
            throw new IllegalStateException("Ticket transition is invalid");
        }
        this.status = TicketStatus.TREATED;
    }

    public void confirmCustomerTreatment() {
        transitionTo(TicketStatus.CUSTOMER_CONFIRMED, TicketStatus.TREATED);
    }

    public void close() {
        if (status != TicketStatus.CREATED
                && status != TicketStatus.RECEIVED
                && status != TicketStatus.TREATED
                && status != TicketStatus.CUSTOMER_CONFIRMED) {
            throw new IllegalStateException("Ticket transition is invalid");
        }
        this.status = TicketStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public void cancel() {
        if (status != TicketStatus.CREATED && status != TicketStatus.RECEIVED) {
            throw new IllegalStateException("Ticket transition is invalid");
        }
        this.status = TicketStatus.CANCELLED;
    }

    private void transitionTo(TicketStatus targetStatus, TicketStatus expectedCurrentStatus) {
        if (status != expectedCurrentStatus) {
            throw new IllegalStateException("Ticket transition is invalid");
        }
        this.status = targetStatus;
    }

    private void recalculateTotalAmount() {
        BigDecimal total = BigDecimal.ZERO;
        boolean hasPricedLine = false;
        for (TicketLine line : lines) {
            if (line.getLineTotalAmount() != null) {
                total = total.add(line.getLineTotalAmount());
                hasPricedLine = true;
            }
        }
        this.totalAmount = hasPricedLine ? total : null;
    }

    private static void validateLocationBelongsToServiceUnit(
            ServiceUnit serviceUnit,
            ServiceUnitLocation serviceUnitLocation) {
        if (!Objects.equals(serviceUnit.getId(), serviceUnitLocation.getServiceUnit().getId())) {
            throw new IllegalArgumentException("Location must belong to the same service unit as ticket");
        }
    }

    private static void validateCreator(User user, String guestName, String guestAccessCodeHash) {
        if (user == null && (guestName == null || guestAccessCodeHash == null)) {
            throw new IllegalArgumentException("Guest ticket requires guest name and access code hash");
        }
        if (user != null && guestAccessCodeHash != null) {
            throw new IllegalArgumentException("Authenticated ticket must not have guest access code hash");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public User getUser() {
        return user;
    }

    public String getGuestName() {
        return guestName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getGuestAccessCodeHash() {
        return guestAccessCodeHash;
    }

    public ServiceUnit getServiceUnit() {
        return serviceUnit;
    }

    public ServiceUnitLocation getServiceUnitLocation() {
        return serviceUnitLocation;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public List<TicketLine> getLines() {
        return List.copyOf(lines);
    }
}
