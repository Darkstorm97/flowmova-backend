package com.flowmova.backend.catalogcategory.infrastructure;

import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.domain.CatalogCategoryStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, UUID> {

    List<CatalogCategory> findByCompanyIdOrderByDisplayOrderAscNameAsc(UUID companyId);

    List<CatalogCategory> findByCompanyIdAndStatusOrderByDisplayOrderAscNameAsc(
            UUID companyId,
            CatalogCategoryStatus status);

    Optional<CatalogCategory> findByIdAndCompanyId(UUID id, UUID companyId);

    boolean existsByCompanyIdAndNameIgnoreCase(UUID companyId, String name);
}
