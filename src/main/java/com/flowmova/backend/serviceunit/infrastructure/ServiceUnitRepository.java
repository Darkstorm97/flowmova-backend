package com.flowmova.backend.serviceunit.infrastructure;

import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceUnitRepository extends JpaRepository<ServiceUnit, UUID> {

    Optional<ServiceUnit> findByIdAndCompanyId(UUID id, UUID companyId);

    Optional<ServiceUnit> findByIdAndCompanyIdAndStatus(UUID id, UUID companyId, ServiceUnitStatus status);

    List<ServiceUnit> findByCompanyIdOrderByNameAsc(UUID companyId);

    Page<ServiceUnit> findByCompanyId(UUID companyId, Pageable pageable);

    List<ServiceUnit> findByCompanyIdAndStatusOrderByNameAsc(UUID companyId, ServiceUnitStatus status);

    Page<ServiceUnit> findByCompanyIdAndStatus(UUID companyId, ServiceUnitStatus status, Pageable pageable);

    List<ServiceUnit> findByCompanyIdAndCompanyStatusAndStatusOrderByNameAsc(
            UUID companyId,
            CompanyStatus companyStatus,
            ServiceUnitStatus status);
}
