package com.flowmova.backend.catalog.infrastructure;

import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.domain.CatalogStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogRepository extends JpaRepository<Catalog, UUID> {

    List<Catalog> findByCompanyIdOrderByNameAsc(UUID companyId);

    List<Catalog> findByCatalogCategoryIdOrderByNameAsc(UUID catalogCategoryId);

    List<Catalog> findByCompanyIdAndStatusOrderByNameAsc(UUID companyId, CatalogStatus status);

    List<Catalog> findByCatalogCategoryIdAndStatusOrderByNameAsc(UUID catalogCategoryId, CatalogStatus status);
}
