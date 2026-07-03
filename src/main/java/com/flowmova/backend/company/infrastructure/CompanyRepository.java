package com.flowmova.backend.company.infrastructure;

import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.domain.CompanyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    List<Company> findByStatus(CompanyStatus status);

    List<Company> findByNameContainingIgnoreCase(String name);

    Page<Company> findByStatus(CompanyStatus status, Pageable pageable);

    Page<Company> findByStatusAndNameContainingIgnoreCase(CompanyStatus status, String name, Pageable pageable);

    Optional<Company> findByIdAndStatus(UUID id, CompanyStatus status);
}
