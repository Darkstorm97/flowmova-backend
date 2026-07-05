package com.flowmova.backend.company.domain;

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
@Table(name = "companies")
public class Company {

    public static final String DEFAULT_CURRENCY = "CAD";
    public static final CompanyBusinessType DEFAULT_BUSINESS_TYPE = CompanyBusinessType.OTHER;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = DEFAULT_CURRENCY;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "business_type", nullable = false, columnDefinition = "company_business_type")
    private CompanyBusinessType businessType = DEFAULT_BUSINESS_TYPE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "company_status")
    private CompanyStatus status = CompanyStatus.DISABLED;

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

    protected Company() {
    }

    public Company(String name, String description, User createdBy) {
        this(name, description, DEFAULT_CURRENCY, DEFAULT_BUSINESS_TYPE, createdBy);
    }

    public Company(String name, String description, String currency, User createdBy) {
        this(name, description, currency, DEFAULT_BUSINESS_TYPE, createdBy);
    }

    public Company(String name, String description, String currency, CompanyBusinessType businessType, User createdBy) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.currency = currency;
        this.businessType = businessType;
        this.createdBy = createdBy;
    }

    public void activate() {
        this.status = CompanyStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCurrency() {
        return currency;
    }

    public CompanyBusinessType getBusinessType() {
        return businessType;
    }

    public CompanyStatus getStatus() {
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
