package com.flowmova.backend.company.api;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.application.CreateCompanyService;
import com.flowmova.backend.company.application.GetActiveCompanyService;
import com.flowmova.backend.company.application.SearchActiveCompaniesService;
import com.flowmova.backend.shared.api.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CreateCompanyService createCompanyService;
    private final SearchActiveCompaniesService searchActiveCompaniesService;
    private final GetActiveCompanyService getActiveCompanyService;

    public CompanyController(
            CreateCompanyService createCompanyService,
            SearchActiveCompaniesService searchActiveCompaniesService,
            GetActiveCompanyService getActiveCompanyService) {
        this.createCompanyService = createCompanyService;
        this.searchActiveCompaniesService = searchActiveCompaniesService;
        this.getActiveCompanyService = getActiveCompanyService;
    }

    @GetMapping
    public PageResponse<CompanyResponse> search(
            @RequestParam(name = "q", required = false) String query,
            Pageable pageable) {
        return PageResponse.from(searchActiveCompaniesService.search(query, pageable));
    }

    @GetMapping("/{companyId}")
    public CompanyResponse get(@PathVariable UUID companyId) {
        return getActiveCompanyService.getActiveCompany(companyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse create(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody CreateCompanyRequest request) {
        return CompanyResponse.from(createCompanyService.createCompany(authenticatedUser, request.toCommand()));
    }
}
