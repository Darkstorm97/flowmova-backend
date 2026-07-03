package com.flowmova.backend.company.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateCompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;
    private final UserRepository userRepository;

    public CreateCompanyService(
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository,
            UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Company createCompany(AuthenticatedUser authenticatedUser, CreateCompanyCommand command) {
        User creator = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User profile not found"));

        Company company = new Company(
                command.name().trim(),
                normalizeDescription(command.description()),
                creator);
        company.activate();

        Company savedCompany = companyRepository.save(company);
        companyUserRepository.save(new CompanyUser(savedCompany.getId(), creator, CompanyRole.ADMIN));

        return savedCompany;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        return description.trim();
    }
}
