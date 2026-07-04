package com.flowmova.backend.item.domain;

import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_unit_id", nullable = false)
    private ServiceUnit serviceUnit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_id", nullable = false)
    private Catalog catalog;

    @Column(name = "price_amount", precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "availability", nullable = false, columnDefinition = "item_availability")
    private ItemAvailability availability = ItemAvailability.AVAILABLE;

    @Column(name = "configured_quantity")
    private Integer configuredQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "item_status")
    private ItemStatus status = ItemStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    protected Item() {
    }

    public Item(
            ServiceUnit serviceUnit,
            Catalog catalog,
            BigDecimal priceAmount,
            ItemAvailability availability,
            Integer configuredQuantity,
            Integer displayOrder) {
        validateSameCompany(serviceUnit, catalog);
        this.id = UUID.randomUUID();
        this.serviceUnit = serviceUnit;
        this.catalog = catalog;
        this.priceAmount = priceAmount;
        this.availability = availability == null ? ItemAvailability.AVAILABLE : availability;
        this.configuredQuantity = configuredQuantity;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
    }

    public void configure(
            BigDecimal priceAmount,
            ItemAvailability availability,
            Integer configuredQuantity,
            Integer displayOrder) {
        this.priceAmount = priceAmount;
        this.availability = availability == null ? ItemAvailability.AVAILABLE : availability;
        this.configuredQuantity = configuredQuantity;
        this.displayOrder = displayOrder == null ? 0 : displayOrder;
    }

    public void archive() {
        this.status = ItemStatus.ARCHIVED;
    }

    private static void validateSameCompany(ServiceUnit serviceUnit, Catalog catalog) {
        if (!Objects.equals(serviceUnit.getCompany().getId(), catalog.getCompany().getId())) {
            throw new IllegalArgumentException("Catalog must belong to the same company as service unit");
        }
    }

    public UUID getId() {
        return id;
    }

    public ServiceUnit getServiceUnit() {
        return serviceUnit;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public ItemAvailability getAvailability() {
        return availability;
    }

    public Integer getConfiguredQuantity() {
        return configuredQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Integer getVersion() {
        return version;
    }
}
