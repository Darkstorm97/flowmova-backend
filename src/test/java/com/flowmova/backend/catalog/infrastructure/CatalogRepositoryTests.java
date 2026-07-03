package com.flowmova.backend.catalog.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.flowmova.backend.catalog.domain.Catalog;
import com.flowmova.backend.catalog.domain.CatalogStatus;
import com.flowmova.backend.catalogcategory.domain.CatalogCategory;
import com.flowmova.backend.catalogcategory.infrastructure.CatalogCategoryRepository;
import com.flowmova.backend.company.domain.Company;
import com.flowmova.backend.company.infrastructure.CompanyRepository;
import com.flowmova.backend.user.domain.User;
import com.flowmova.backend.user.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CatalogRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CatalogCategoryRepository catalogCategoryRepository;

    @Autowired
    private CatalogRepository catalogRepository;

    @Test
    void savesListsAndFiltersCatalogs() {
        User owner = userRepository.save(new User(
                "catalog-owner.%s@flowmova.test".formatted(UUID.randomUUID()),
                "$2a$10$placeholder",
                "Catalog",
                "Owner"));
        Company company = companyRepository.save(new Company(
                "Catalog Company",
                "Company with catalogs",
                owner));
        Company otherCompany = companyRepository.save(new Company(
                "Other Catalog Company",
                "Other company with catalogs",
                owner));
        CatalogCategory services = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Services",
                "Service offers",
                10,
                owner));
        CatalogCategory bundles = catalogCategoryRepository.save(new CatalogCategory(
                company,
                "Bundles",
                "Bundle offers",
                20,
                owner));
        CatalogCategory otherServices = catalogCategoryRepository.save(new CatalogCategory(
                otherCompany,
                "Services",
                "Other service offers",
                10,
                owner));

        Catalog basic = catalogRepository.save(new Catalog(
                company,
                services,
                "Basic Cleaning",
                "Basic service",
                "https://cdn.flowmova.test/basic-cleaning.png",
                new BigDecimal("49.99"),
                owner));
        Catalog premium = catalogRepository.save(new Catalog(
                company,
                services,
                "Premium Cleaning",
                "Premium service",
                null,
                new BigDecimal("89.50"),
                owner));
        premium.archive();
        Catalog bundle = catalogRepository.save(new Catalog(
                company,
                bundles,
                "Starter Bundle",
                "Starter package",
                null,
                null,
                owner));
        catalogRepository.save(new Catalog(
                otherCompany,
                otherServices,
                "Other Basic Cleaning",
                "Other company service",
                null,
                new BigDecimal("39.99"),
                owner));

        List<Catalog> companyCatalogs = catalogRepository.findByCompanyIdOrderByNameAsc(company.getId());
        List<Catalog> serviceCatalogs = catalogRepository.findByCatalogCategoryIdOrderByNameAsc(services.getId());
        List<Catalog> activeCompanyCatalogs = catalogRepository.findByCompanyIdAndStatusOrderByNameAsc(
                company.getId(),
                CatalogStatus.ACTIVE);
        List<Catalog> activeServiceCatalogs = catalogRepository.findByCatalogCategoryIdAndStatusOrderByNameAsc(
                services.getId(),
                CatalogStatus.ACTIVE);

        assertThat(companyCatalogs).containsExactly(basic, premium, bundle);
        assertThat(serviceCatalogs).containsExactly(basic, premium);
        assertThat(activeCompanyCatalogs).containsExactly(basic, bundle);
        assertThat(activeServiceCatalogs).containsExactly(basic);
        assertThat(basic.getStatus()).isEqualTo(CatalogStatus.ACTIVE);
        assertThat(premium.getStatus()).isEqualTo(CatalogStatus.ARCHIVED);
        assertThat(basic.getPriceAmount()).isEqualByComparingTo("49.99");
        assertThat(bundle.getPriceAmount()).isNull();
        assertThat(basic.getImageUrl()).isEqualTo("https://cdn.flowmova.test/basic-cleaning.png");
        assertThat(basic.getCreatedAt()).isNotNull();
        assertThat(basic.getUpdatedAt()).isNotNull();
        assertThat(basic.getCreatedBy()).isEqualTo(owner);
        assertThat(basic.getVersion()).isNotNull();
    }
}
