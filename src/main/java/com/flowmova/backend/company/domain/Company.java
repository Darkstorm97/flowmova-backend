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
import java.math.BigDecimal;
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

    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "region", length = 120)
    private String region;

    @Column(name = "postal_code", length = 40)
    private String postalCode;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

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
        this(
                name,
                description,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                currency,
                businessType,
                createdBy);
    }

    public Company(
            String name,
            String description,
            String addressLine1,
            String addressLine2,
            String city,
            String region,
            String postalCode,
            String country,
            BigDecimal latitude,
            BigDecimal longitude,
            String currency,
            CompanyBusinessType businessType,
            User createdBy) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.region = region;
        this.postalCode = postalCode;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getCity() {
        return city;
    }

    public String getRegion() {
        return region;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
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
