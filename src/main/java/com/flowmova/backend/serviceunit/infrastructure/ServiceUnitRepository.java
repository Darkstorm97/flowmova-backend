package com.flowmova.backend.serviceunit.infrastructure;

import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.domain.ServiceUnitStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceUnitRepository extends JpaRepository<ServiceUnit, UUID> {

    List<ServiceUnit> findByCompanyIdOrderByNameAsc(UUID companyId);

    List<ServiceUnit> findByCompanyIdAndStatusOrderByNameAsc(UUID companyId, ServiceUnitStatus status);

    List<ServiceUnit> findByCompanyIdAndCompanyStatusAndStatusOrderByNameAsc(
            UUID companyId,
            CompanyStatus companyStatus,
            ServiceUnitStatus status);
}
