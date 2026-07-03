package com.flowmova.backend.catalog.domain;

import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "catalogs")
public class Catalog {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "catalog_category_id", nullable = false)
    private CatalogCategory catalogCategory;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "image_url", length = 2000)
    private String imageUrl;

    @Column(name = "price_amount", precision = 12, scale = 2)
    private BigDecimal priceAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "catalog_status")
    private CatalogStatus status = CatalogStatus.ACTIVE;

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

    protected Catalog() {
    }

    public Catalog(
            Company company,
            CatalogCategory catalogCategory,
            String name,
            String description,
            String imageUrl,
            BigDecimal priceAmount,
            User createdBy) {
        this.id = UUID.randomUUID();
        this.company = company;
        this.catalogCategory = catalogCategory;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.priceAmount = priceAmount;
        this.createdBy = createdBy;
    }

    public void archive() {
        this.status = CatalogStatus.ARCHIVED;
    }

    public UUID getId() {
        return id;
    }

    public Company getCompany() {
        return company;
    }

    public CatalogCategory getCatalogCategory() {
        return catalogCategory;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getPriceAmount() {
        return priceAmount;
    }

    public CatalogStatus getStatus() {
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
