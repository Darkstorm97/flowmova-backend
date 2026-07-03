package com.flowmova.backend.company.application;

import com.flowmova.backend.company.api.CompanyResponse;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GetActiveCompanyService {

    private final CompanyRepository companyRepository;

    public GetActiveCompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public CompanyResponse getActiveCompany(UUID companyId) {
        return companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .map(CompanyResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }
}
