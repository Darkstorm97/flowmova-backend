package com.flowmova.backend.item.application;

import com.flowmova.backend.auth.domain.AuthenticatedUser;
import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.infrastructure.CatalogRepository;
import com.flowmova.backend.company.domain.CompanyStatus;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.companyaccess.domain.CompanyRole;
import com.flowmova.backend.companyaccess.domain.CompanyUser;
import com.flowmova.backend.companyaccess.domain.CompanyUserStatus;
import com.flowmova.backend.companyaccess.infrastructure.CompanyUserRepository;
import com.flowmova.backend.item.api.ItemResponse;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.infrastructure.ItemRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreateItemService {

    private final ItemRepository itemRepository;
    private final ServiceUnitRepository serviceUnitRepository;
    private final CatalogRepository catalogRepository;
    private final CompanyRepository companyRepository;
    private final CompanyUserRepository companyUserRepository;

    public CreateItemService(
            ItemRepository itemRepository,
            ServiceUnitRepository serviceUnitRepository,
            CatalogRepository catalogRepository,
            CompanyRepository companyRepository,
            CompanyUserRepository companyUserRepository) {
        this.itemRepository = itemRepository;
        this.serviceUnitRepository = serviceUnitRepository;
        this.catalogRepository = catalogRepository;
        this.companyRepository = companyRepository;
        this.companyUserRepository = companyUserRepository;
    }

    @Transactional
    public ItemResponse create(
            UUID companyId,
            UUID serviceUnitId,
            AuthenticatedUser authenticatedUser,
            CreateItemCommand command) {
        companyRepository.findByIdAndStatus(companyId, CompanyStatus.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));

        requireActiveAdmin(companyId, authenticatedUser.userId());

        ServiceUnit serviceUnit = serviceUnitRepository.findByIdAndCompanyId(serviceUnitId, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service unit not found"));

        Catalog catalog = catalogRepository.findByIdAndCompanyId(command.catalogId(), companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catalog is invalid"));

        if (itemRepository.existsByServiceUnitIdAndCatalogId(serviceUnitId, command.catalogId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Catalog is already associated with service unit");
        }

        BigDecimal priceAmount = command.priceAmount() == null ? catalog.getPriceAmount() : command.priceAmount();
        ItemAvailability availability = command.availability() == null
                ? ItemAvailability.AVAILABLE
                : command.availability();

        Item item = new Item(
                serviceUnit,
                catalog,
                priceAmount,
                availability,
                command.configuredQuantity(),
                command.displayOrder());

        return ItemResponse.from(itemRepository.saveAndFlush(item));
    }

    private void requireActiveAdmin(UUID companyId, UUID userId) {
        CompanyUser companyUser = companyUserRepository.findByCompanyIdAndUserId(companyId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required"));

        if (companyUser.getStatus() != CompanyUserStatus.ACTIVE || companyUser.getRole() != CompanyRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin role is required");
        }
    }
}
