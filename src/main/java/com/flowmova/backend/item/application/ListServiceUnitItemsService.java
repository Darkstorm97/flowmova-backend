package com.flowmova.backend.item.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.item.api.ItemResponse;
import com.flowmova.backend.item.infrastructure.ItemRepository;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ListServiceUnitItemsService {

    private final ItemRepository itemRepository;
    private final ServiceUnitRepository serviceUnitRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;

    public ListServiceUnitItemsService(
            ItemRepository itemRepository,
            ServiceUnitRepository serviceUnitRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository) {
        this.itemRepository = itemRepository;
        this.serviceUnitRepository = serviceUnitRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> list(UUID companyId, UUID serviceUnitId, AuthenticatedUser authenticatedUser) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        serviceUnitRepository.findByIdAndCompanyId(serviceUnitId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service unit not found"));

        return itemRepository.findByServiceUnitIdOrderByDisplayOrderAscCatalogNameAsc(serviceUnitId)
                .stream()
                .map(ItemResponse::from)
                .toList();
    }

    private void requireActiveAdmin(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE || companyUser.getRole() != CompanyRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required");
        }
    }
}
