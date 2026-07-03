package com.flowmova.backend.company.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.application.CreateCompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CreateCompanyService createCompanyService;

    public CompanyController(CreateCompanyService createCompanyService) {
        this.createCompanyService = createCompanyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCompanyRequest request) {
        return CompanyResponse.from(createCompanyService.createCompany(authenticatedUser, request.toCommand()));
    }
}
