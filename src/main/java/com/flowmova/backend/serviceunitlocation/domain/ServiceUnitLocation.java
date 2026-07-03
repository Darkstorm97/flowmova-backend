package com.flowmova.backend.serviceunitlocation.domain;

import com.flowmova.backend.serviceunit.domain.ServiceUnit;
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
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "service_unit_locations")
public class ServiceUnitLocation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_unit_id", nullable = false)
    private ServiceUnit serviceUnit;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "type", nullable = false, columnDefinition = "service_unit_location_type")
    private ServiceUnitLocationType type = ServiceUnitLocationType.CUSTOM;

    @Column(name = "is_default", nullable = false)
    private boolean defaultLocation;

    @Column(name = "public_access_slug", nullable = false, length = 120)
    private String publicAccessSlug;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "service_unit_location_status")
    private ServiceUnitLocationStatus status = ServiceUnitLocationStatus.ACTIVE;

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

    protected ServiceUnitLocation() {
    }

    public ServiceUnitLocation(
            ServiceUnit serviceUnit,
            String name,
            String description,
            ServiceUnitLocationType type,
            boolean defaultLocation,
            String publicAccessSlug,
            User createdBy) {
        this.id = UUID.randomUUID();
        this.serviceUnit = serviceUnit;
        this.name = name;
        this.description = description;
        this.type = type;
        this.defaultLocation = defaultLocation;
        this.publicAccessSlug = publicAccessSlug;
        this.createdBy = createdBy;
    }

    public void archive(User updatedBy) {
        this.status = ServiceUnitLocationStatus.ARCHIVED;
        this.updatedBy = updatedBy;
    }

    public UUID getId() {
        return id;
    }

    public ServiceUnit getServiceUnit() {
        return serviceUnit;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ServiceUnitLocationType getType() {
        return type;
    }

    public boolean isDefaultLocation() {
        return defaultLocation;
    }

    public String getPublicAccessSlug() {
        return publicAccessSlug;
    }

    public ServiceUnitLocationStatus getStatus() {
        return status;
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
