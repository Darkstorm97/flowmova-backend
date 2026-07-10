package com.flowmova.backend.serviceunit.domain;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "service_units")
public class ServiceUnit {

    private static final String ALLOW_TICKET_WITHOUT_ITEMS_SETTING = "allowTicketWithoutItems";

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "service_unit_type")
    private ServiceUnitType type = ServiceUnitType.TICKET_QUEUE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "service_unit_status")
    private ServiceUnitStatus status = ServiceUnitStatus.CLOSED;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "ticket_creation_guard_mode", nullable = false, columnDefinition = "ticket_creation_guard_mode")
    private TicketCreationGuardMode ticketCreationGuardMode = TicketCreationGuardMode.NONE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "creation_entry_mode", nullable = false, columnDefinition = "service_unit_creation_entry_mode")
    private ServiceUnitCreationEntryMode creationEntryMode = ServiceUnitCreationEntryMode.PUBLIC_AND_QR;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> settings = new LinkedHashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    protected ServiceUnit() {
    }

    public ServiceUnit(
            Company company,
            String name,
            String description,
            String location,
            Map<String, Object> settings,
            User createdBy) {
        this.id = UUID.randomUUID();
        this.company = company;
        this.name = name;
        this.description = description;
        this.location = location;
        this.settings = settings == null ? new LinkedHashMap<>() : new LinkedHashMap<>(settings);
        this.createdBy = createdBy;
    }

    public void open(User updatedBy) {
        this.status = ServiceUnitStatus.OPEN;
        this.updatedBy = updatedBy;
    }

    public void close(User updatedBy) {
        this.status = ServiceUnitStatus.CLOSED;
        this.updatedBy = updatedBy;
    }

    public void archive(User updatedBy) {
        this.status = ServiceUnitStatus.ARCHIVED;
        this.updatedBy = updatedBy;
    }

    public void update(
            String name,
            String description,
            String location,
            TicketCreationGuardMode ticketCreationGuardMode,
            ServiceUnitCreationEntryMode creationEntryMode,
            Boolean allowTicketWithoutItems,
            User updatedBy) {
        this.name = name;
        this.description = description;
        this.location = location;
        if (ticketCreationGuardMode != null) {
            this.ticketCreationGuardMode = ticketCreationGuardMode;
        }
        if (creationEntryMode != null) {
            this.creationEntryMode = creationEntryMode;
        }
        setAllowTicketWithoutItems(allowTicketWithoutItems);
        this.updatedBy = updatedBy;
    }

    public void setTicketCreationGuardMode(TicketCreationGuardMode ticketCreationGuardMode) {
        this.ticketCreationGuardMode = ticketCreationGuardMode == null
                ? TicketCreationGuardMode.NONE
                : ticketCreationGuardMode;
    }

    public void setCreationEntryMode(ServiceUnitCreationEntryMode creationEntryMode) {
        this.creationEntryMode = creationEntryMode == null
                ? ServiceUnitCreationEntryMode.PUBLIC_AND_QR
                : creationEntryMode;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public ServiceUnitType getType() {
        return type;
    }

    public ServiceUnitStatus getStatus() {
        return status;
    }

    public TicketCreationGuardMode getTicketCreationGuardMode() {
        return ticketCreationGuardMode;
    }

    public ServiceUnitCreationEntryMode getCreationEntryMode() {
        return creationEntryMode;
    }

    public Map<String, Object> getSettings() {
        return Map.copyOf(settings);
    }

    public boolean isTicketWithoutItemsAllowed() {
        Object value = settings.get(ALLOW_TICKET_WITHOUT_ITEMS_SETTING);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return true;
    }

    public void setAllowTicketWithoutItems(Boolean allowTicketWithoutItems) {
        if (allowTicketWithoutItems == null) {
            return;
        }
        Map<String, Object> updatedSettings = new LinkedHashMap<>(settings);
        updatedSettings.put(ALLOW_TICKET_WITHOUT_ITEMS_SETTING, allowTicketWithoutItems);
        settings = updatedSettings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public Integer getVersion() {
        return version;
    }
}
