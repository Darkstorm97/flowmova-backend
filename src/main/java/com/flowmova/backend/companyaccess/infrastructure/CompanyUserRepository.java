package com.flowmova.backend.companyaccess.infrastructure;

import com.flowmova.backend.companyaccess.domain.CompanyUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyUserRepository extends JpaRepository<CompanyUser, UUID> {

    List<CompanyUser> findByCompanyId(UUID companyId);

    List<CompanyUser> findByUserId(UUID userId);

    Optional<CompanyUser> findByCompanyIdAndUserId(UUID companyId, UUID userId);

    boolean existsByCompanyIdAndUserId(UUID companyId, UUID userId);
}
