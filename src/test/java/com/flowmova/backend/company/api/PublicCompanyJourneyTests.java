package com.flowmova.backend.company.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.infrastructure.CatalogRepository;
import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.item.domain.Item;
import com.flowmova.backend.item.domain.ItemAvailability;
import com.flowmova.backend.item.infrastructure.ItemRepository;
import com.flowmova.backend.serviceunit.domain.ServiceUnit;
import com.flowmova.backend.serviceunit.infrastructure.ServiceUnitRepository;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocation;
import com.flowmova.backend.serviceunitlocation.domain.ServiceUnitLocationType;
import com.flowmova.backend.serviceunitlocation.infrastructure.ServiceUnitLocationRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PublicCompanyJourneyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CatalogCategoryRepository catalogCategoryRepository;

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private ServiceUnitRepository serviceUnitRepository;

    @Autowired
    private ServiceUnitLocationRepository serviceUnitLocationRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void publicUserSearchesCompanyAndConsultsActiveCatalogsAndOpenServiceUnitsWithoutJwt() throws Exception {
        User owner = user("public-company-journey");
        String uniquePrefix = "Public Journey %s".formatted(UUID.randomUUID());
        Company company = activeCompany(uniquePrefix + " Visible", "Visible public company", owner);
        Company disabledCompany = companyRepository.save(new Company(
                uniquePrefix + " Hidden",
                "Hidden disabled company",
                owner));

        CatalogCategory visibleCategory = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Visible Category",
                "Category visible publicly",
                10,
                owner));
        CatalogCategory archivedCategory = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Archived Category",
                "Hidden archived category",
                20,
                owner));
        archivedCategory.archive();
        catalogCategoryRepository.saveAndFlush(archivedCategory);

        Catalog visibleCatalog = catalogRepository.save(new Catalog(
                company,
                visibleCategory,
                "Visible Catalog",
                "Catalog visible publicly",
                null,
                new BigDecimal("12.50"),
                owner));
        Catalog archivedCatalog = catalogRepository.save(new Catalog(
                company,
                visibleCategory,
                "Archived Catalog",
                "Hidden archived catalog",
                null,
                null,
                owner));
        archivedCatalog.archive();
        catalogRepository.saveAndFlush(archivedCatalog);

        ServiceUnit closedServiceUnit = serviceUnitRepository.save(new ServiceUnit(
                company,
                "Closed Queue",
                "Hidden closed queue",
                null,
                null,
                owner));
        serviceUnitLocationRepository.save(new ServiceUnitLocation(
                closedServiceUnit,
                "Closed default",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                owner));

        ServiceUnit openServiceUnit = new ServiceUnit(
                company,
                "Open Queue",
                "Visible open queue",
                null,
                null,
                owner);
        openServiceUnit.open(owner);
        openServiceUnit = serviceUnitRepository.save(openServiceUnit);
        ServiceUnitLocation openDefaultLocation = serviceUnitLocationRepository.save(new ServiceUnitLocation(
                openServiceUnit,
                "Open default",
                null,
                ServiceUnitLocationType.DEFAULT,
                true,
                "loc-%s".formatted(UUID.randomUUID()),
                owner));
        Item visibleItem = itemRepository.save(new Item(
                openServiceUnit,
                visibleCatalog,
                new BigDecimal("12.50"),
                ItemAvailability.AVAILABLE,
                null,
                0));

        mockMvc.perform(get("/api/companies")
                        .param("q", uniquePrefix)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == '%s')]".formatted(company.getId())).exists())
                .andExpect(jsonPath("$.items[?(@.id == '%s')]".formatted(disabledCompany.getId())).doesNotExist());

        mockMvc.perform(get("/api/companies/{companyId}", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(company.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/companies/{companyId}/catalog-categories", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visibleCategory.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(archivedCategory.getId())).doesNotExist());

        mockMvc.perform(get("/api/companies/{companyId}/catalogs", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visibleCatalog.getId().toString()))
                .andExpect(jsonPath("$[0].catalogCategoryId").value(visibleCategory.getId().toString()))
                .andExpect(jsonPath("$[0].priceAmount").value(12.50))
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(archivedCatalog.getId())).doesNotExist());

        mockMvc.perform(get("/api/companies/{companyId}/service-units", company.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(openServiceUnit.getId().toString()))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].defaultLocation.id").value(openDefaultLocation.getId().toString()))
                .andExpect(jsonPath("$[?(@.id == '%s')]".formatted(closedServiceUnit.getId())).doesNotExist());

        mockMvc.perform(get(
                        "/api/companies/{companyId}/service-units/{serviceUnitId}",
                        company.getId(),
                        openServiceUnit.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(openServiceUnit.getId().toString()))
                .andExpect(jsonPath("$.defaultLocation.id").value(openDefaultLocation.getId().toString()))
                .andExpect(jsonPath("$.items[0].id").value(visibleItem.getId().toString()));
    }

    private User user(String emailPrefix) {
        return userRepository.save(new User(
                "%s.%s@flowmova.test".formatted(emailPrefix, UUID.randomUUID()),
                passwordEncoder.encode("Password123!"),
                "Public",
                "User"));
    }

    private Company activeCompany(String name, String description, User createdBy) {
        Company company = new Company(name, description, createdBy);
        company.activate();
        return companyRepository.save(company);
    }
}
